package no.kvikshaug.gh;

import com.google.gson.*;
import com.sun.net.httpserver.*;
import no.kvikshaug.gh.exceptions.PreferenceNotSetException;
import no.kvikshaug.gh.util.Web;
import org.apache.commons.io.IOUtils;
import org.jibble.pircbot.Colors;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

import static java.net.URLDecoder.decode;

public class GithubPostReceiveServer {

    private Grouphug bot;
    private HttpServer server;

    public GithubPostReceiveServer(Grouphug bot) {
        this.bot = bot;
    }

    private static String colorize(String color, String s) {
        return color + s + Colors.NORMAL;
    }

    public void start() {
        String url;
        int port;
        try {
            url = Config.githubHookUrl();
            port = Config.githubHookPort();
        } catch (PreferenceNotSetException e) {
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
        if(this.server != null) {
            this.server.stop(0);
        }
    }

    private class GithubPayloadHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String requestMethod = exchange.getRequestMethod();
            if (requestMethod.equalsIgnoreCase("POST")) {
                InputStream payloadStream = exchange.getRequestBody();
                StringWriter writer = new StringWriter();
                IOUtils.copy(payloadStream, writer, "UTF-8");
                String body = decode(writer.toString(), "UTF-8");
                if (body.startsWith("payload=")) {
                    // There's no free lunch with HttpExchange; hack to get the payload POST argument :3
                    body = body.substring(8);
                }

                Payload payload = jsonToPayload(body);
                if (payload == null) { return; }

                URI requestURI = exchange.getRequestURI();
                String channel = '#' + requestURI.getPath().substring(1);

                if (payload.created) {
                    bot.msg(channel, createdBranchMessage(payload));
                } else if (payload.deleted) {
                    bot.msg(channel, deletedBranchMessage(payload));
                }

                if (payload.commits.size() > 0) {
                    bot.msg(channel, pushMessage(payload));
                }

                Headers responseHeaders = exchange.getResponseHeaders();
                responseHeaders.set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
            }
        }


    }

    private static Payload jsonToPayload(String body) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(DateTime.class, new DateTimeTypeConverter());
        Gson gson = gsonBuilder.create();
        Payload payload = null;
        try {
            payload = gson.fromJson(body, Payload.class);
        } catch (JsonParseException jpe) {
            jpe.printStackTrace(System.err);
        }
        return payload;
    }

    private static String createdBranchMessage(Payload payload) {
        StringBuilder message = new StringBuilder();
        message.append(payload.prefix()).append(": new branch created by ").append(payload.pusher.name);
        return message.toString();
    }

    private static String deletedBranchMessage(Payload payload) {
        StringBuilder message = new StringBuilder();
        message.append(payload.prefix()).append(": branch deleted by ").append(payload.pusher.name);
        return message.toString();
    }

    private static String pushMessage(Payload payload) {
        Commit head = payload.getHead();
        String headHashShort = head.id.substring(0, 6);
        int commitCount = payload.commits.size();

        StringBuilder message = new StringBuilder();
        message.append(payload.prefix()).append(": ").append(payload.pusher.name).append(" pushed ")
               .append(colorize(Colors.BOLD, headHashShort)).append(" by ").append(head.author.name).append(": \"").append(head.getShortMessage()).append('"');
        if (commitCount > 1) {
            int additionalCommits = commitCount - 1;
            message.append(" + ").append(additionalCommits);
            message.append((additionalCommits >= 2) ? " more commits" : " more commit");
        }
        message.append(" — ").append(Web.getBitlyURL(payload.compare));

        return message.toString();
    }

    private static class Payload {
        private String after;
        private String before;
        private String base;
        private List<Commit> commits;
        private String compare;
        private boolean created;
        private boolean deleted;
        private boolean forced;
        private User pusher;
        private String ref;
        private Repository repository;

        public Commit getHead() {
            for (Commit commit : commits) {
                if (commit.id.equals(after)) {
                    return commit;
                }
            }
            return null;
        }

        public String getAffectedRefName() {
            return ref.substring(ref.lastIndexOf('/') + 1);
        }

        public String prefix() {
            return repository.getRepoName() + ' ' + getAffectedRefName();
        }
    }

    private static class Repository {
        private DateTime created_at;
        private String description;
        private boolean fork;
        private int forks;
        private boolean has_downloads;
        private boolean has_issues;
        private boolean has_wiki;
        private String homepage;
        private String language;
        private String name;
        private int open_issues;
        private User owner;
        private int watchers;
        private boolean _private;
        private DateTime pushed_at;
        private int size;
        private String url;

        public String getRepoName() {
            return owner.name + '/' + name;
        }
    }

    private static class User {
        private String email;
        private String name;
    }

    private static class Commit {
        private List<String> added;
        private User author;
        private boolean distinct;
        private String id;
        private String message;
        private List<String> modified;
        private List<String> removed;
        private DateTime timestamp;
        private String url;


        public String getShortMessage() {
            if (message.indexOf('\n') != -1) {
                return message.substring(0, message.indexOf('\n'));
            } else {
                return message;
            }
        }
    }

    private class DateTimeDeserializer implements JsonDeserializer<DateTime> {
        public DateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return new DateTime(json.getAsJsonPrimitive().getAsString());
        }
    }

    // originally from: http://sites.google.com/site/gson/gson-type-adapters-for-common-classes-1
    private static class DateTimeTypeConverter
            implements JsonSerializer<DateTime>, JsonDeserializer<DateTime> {
        public JsonElement serialize(DateTime src, Type srcType, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }

        public DateTime deserialize(JsonElement json, Type type, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                try {
                    return new DateTime(json.getAsString());
                } catch (IllegalArgumentException e) {
                    // May be it came in formatted as a java.util.Date, so try that
                    Date date = context.deserialize(json, Date.class);
                    return new DateTime(date);
                }
            } catch (JsonParseException jpe) {
                // Github seems to use a format that doesn't work with java.util.Date
                SimpleDateFormat githubFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                try {
                    return new DateTime(githubFormat.parse(json.getAsString()));
                } catch (ParseException eFirst) {
                    // Github seems to use ANOTHER format that doesn't work with java.util.Date (what the hell?)
                    SimpleDateFormat secondGithubFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z");
                    try {
                        return new DateTime(secondGithubFormat.parse(json.getAsString()));
                    } catch (ParseException eSecond) {
                        System.err.println("GithubPostReceive: Unable to parse date!");
                        eSecond.printStackTrace(System.err);
                    }
                }
                return null;
            }
        }
    }

    // For debugging payloads
    public static void main(String[] args) {
        InputStreamReader stdin = new InputStreamReader(System.in);
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(stdin, writer);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        String json = writer.toString();
        System.out.println("JSON: " + json);
        Payload payload = jsonToPayload(json);
        System.out.println(payload);
    }
}


