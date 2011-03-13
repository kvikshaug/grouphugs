package no.kvikshaug.gh;

import static java.net.URLDecoder.decode;

import com.google.gson.Gson;
import com.sun.net.httpserver.*;
import no.kvikshaug.gh.exceptions.GithubHookDisabledException;
import org.apache.commons.io.IOUtils;

import org.jibble.pircbot.Colors;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class GithubPostReceiveServer {

    private Grouphug bot;
    private HttpServer server;

    public GithubPostReceiveServer(Grouphug bot) {
        this.bot = bot;
    }

    private String colorize(String color, String s) {
        return color + s + Colors.NORMAL;
    }

    public void start() {
        String url;
        int port;
        try {
            url = Config.githubHookUrl();
            port = Config.githubHookPort();
        } catch (GithubHookDisabledException e) {
            System.out.println("GithubPostReceive: Disabled.");
            System.out.println("GithubPostReceive: " + e.getMessage());
            return;
        }
        InetSocketAddress addr = new InetSocketAddress(url, port);
        try {
            this.server = HttpServer.create(addr, 0);
        } catch (IOException e) {
            System.err.println("GithubPostReceive: Unable to start server on " + addr + '!');
            e.printStackTrace(System.err);
        }

        if (this.server == null) {
            return;
        }

        List<HttpContext> contexts = new ArrayList<HttpContext>();
        for (String channel : Config.channels()) {
            HttpContext context = this.server.createContext("/" + channel.substring(1), new GithubPayloadHandler());
            contexts.add(context);
        }

        this.server.setExecutor(Executors.newCachedThreadPool());
        this.server.start();
        for (HttpContext context : contexts) {
            System.out.println("GithubPostReceive: Server is listening on " + addr + context.getPath());
        }
    }

    public void stop() {
        this.server.stop(0);
    }

    private class GithubPayloadHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String requestMethod = exchange.getRequestMethod();
            if (requestMethod.equalsIgnoreCase("POST")) {
                InputStream payloadStream = exchange.getRequestBody();
                StringWriter writer = new StringWriter();
                IOUtils.copy(payloadStream, writer, "UTF-8");
                String body = decode(writer.toString());
                if (body.startsWith("payload=")) {
                    System.out.println("GithubPostReceive: stripped 'payload='");
                    body = body.substring(8);
                }

                Gson gson = new Gson();
                Payload payload = gson.fromJson(body, Payload.class);

                URI requestURI = exchange.getRequestURI();
                String channel = '#' + requestURI.getPath().substring(1);

                Commit head = payload.getHead();
                String headHashShort = head.getId().substring(0, 6);
                String author = head.getAuthor().getName();
                String repo = payload.getRepository().getOwner().getName() + '/' + payload.getRepository().getName();
                String headMessage = head.getMessage();
                String branch = payload.getRef().substring(payload.getRef().lastIndexOf('/') + 1);
                String headUrl = head.getUrl().substring(0, head.getUrl().length() - 34);
                if (headUrl.startsWith("http:")) {
                    headUrl = headUrl.replaceFirst("http:", "https:");
                }

                StringBuilder message = new StringBuilder();
                message.append(' ').append(repo).append(' ').append(branch)
                        .append(": ").append(author)
                        .append(" ").append(colorize(Colors.BOLD, headHashShort)).append(" \"").append(headMessage).append('"');
                if (payload.getCommits().size() > 1) {
                    message.append(" (+ ").append(payload.commits.size());
                    message.append((payload.getCommits().size() > 3) ? " more commit" : " more commits");
                    message.append(')');
                }
                message.append(" — ").append(headUrl);

                bot.sendMessageChannel(channel, message.toString());

                Headers responseHeaders = exchange.getResponseHeaders();
                responseHeaders.set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(200, 0);
            }
        }
    }

    private static class Payload {
        private String before;
        private Repository repository;
        private List<Commit> commits;
        private String after;
        private String ref;

        public Commit getHead() {
            for (Commit commit : commits) {
                if (commit.id.equals(after)) {
                    return commit;
                }
            }
            return null;
        }

        public String getBefore() {
            return before;
        }

        public Repository getRepository() {
            return repository;
        }

        public List<Commit> getCommits() {
            return commits;
        }

        public String getAfter() {
            return after;
        }

        public String getRef() {
            return ref;
        }
    }

    private static class Repository {
        private String name;
        private String url;
        private String pledgie;
        private String description;
        private int watchers;
        private int forks;
        private int _private;
        private User owner;



        public String getUrl() {
            return url;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public int getWatchers() {
            return watchers;
        }

        public int getForks() {
            return forks;
        }

        public int getPrivate() {
            return _private;
        }

        public User getOwner() {
            return owner;
        }

        public String getPledgie() {
            return pledgie;
        }
    }

    private static class User {
        private String email;
        private String name;

        public String getEmail() {
            return email;
        }

        public String getName() {
            return name;
        }
    }

    private static class Commit {
        private String id;
        private String url;
        private User author;
        private String message;
        private String timestamp;
        private List<String> added;
        private List<String> removed;
        private List<String> modified;

        public String getId() {
            return id;
        }

        public String getUrl() {
            return url;
        }

        public User getAuthor() {
            return author;
        }

        public String getMessage() {
            return message;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public List<String> getAdded() {
            return added;
        }

        public List<String> getRemoved() {
            return removed;
        }

        public List<String> getModified() {
            return modified;
        }
    }
}


