package no.kvikshaug.gh.github;

import com.google.gson.*;
import com.sun.net.httpserver.*;
import no.kvikshaug.gh.Config;
import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.exceptions.PreferenceNotSetException;
import no.kvikshaug.gh.github.json.Commit;
import no.kvikshaug.gh.github.json.DateTimeTypeConverter;
import no.kvikshaug.gh.github.json.Payload;
import no.kvikshaug.gh.util.Web;
import org.apache.bcel.verifier.structurals.ExceptionHandler;
import org.apache.commons.io.IOUtils;
import org.jibble.pircbot.Colors;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
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
            HttpContext context = this.server.createContext("/" + channel.substring(1), new GithubPayloadHandler(channel));
            contexts.add(context);
        }

        this.server.setExecutor(Executors.newCachedThreadPool());
        this.server.start();
        for (HttpContext context : contexts) {
            System.out.println("GithubPostReceive: Server is listening on " + addr + context.getPath());
        }
    }

    public void stop() {
        if (this.server != null) {
            this.server.stop(0);
        }
    }

    private class GithubPayloadHandler implements HttpHandler {
        private BasicAuthenticator authenticator;

        private GithubPayloadHandler(String channel) {
            try {
                String username = Config.githubHookUsername(channel);
                String password = Config.githubHookPassword(channel);
                this.authenticator = new GithubBasicAuthenticator(
                        String.format("Github post-receive hook for %s.", channel),
                        username, password);
            } catch (PreferenceNotSetException pnse) {
                this.authenticator = null;
            }
        }

        private boolean hasHttpAuth() {
            return this.authenticator != null;
        }

        public void handle(HttpExchange exchange) throws IOException {
            if (this.hasHttpAuth()) {
                Authenticator.Result result = this.authenticator.authenticate(exchange);
                if (!(result instanceof Authenticator.Success)) { // Authentication failed
                    int responseCode = 401;
                    if (result instanceof Authenticator.Retry) {
                        responseCode = ((Authenticator.Retry)result).getResponseCode();
                    } else if (result instanceof Authenticator.Failure) {
                        responseCode = ((Authenticator.Failure)result).getResponseCode();
                    }
                    Headers responseHeaders = exchange.getResponseHeaders();
                    responseHeaders.set("Content-Type", "text/plain");
                    exchange.sendResponseHeaders(responseCode, 0);
                    exchange.close();
                    return;
                }
            }

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
                if (payload == null) {
                    return;
                }

                URI requestURI = exchange.getRequestURI();
                String channel = '#' + requestURI.getPath().substring(1);

                if (payload.isCreated()) {
                    bot.msg(channel, createdBranchMessage(payload));
                } else if (payload.isDeleted()) {
                    bot.msg(channel, deletedBranchMessage(payload));
                }

                if (payload.getCommits().size() > 0) {
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
        message.append(payload.prefix()).append(": new branch created by ").append(payload.getPusher().getName());
        return message.toString();
    }

    private static String deletedBranchMessage(Payload payload) {
        StringBuilder message = new StringBuilder();
        message.append(payload.prefix()).append(": branch deleted by ").append(payload.getPusher().getName());
        return message.toString();
    }

    private static String pushMessage(Payload payload) {
        Commit head = payload.getHead();
        String headHashShort = head.getId().substring(0, 6);
        int commitCount = payload.getCommits().size();

        StringBuilder message = new StringBuilder();
        message.append(payload.prefix()).append(": ").append(payload.getPusher().getName()).append(" pushed ")
                .append(colorize(Colors.BOLD, headHashShort)).append(" by ").append(head.getAuthor().getName()).append(": \"").append(head.getShortMessage()).append('"');
        if (commitCount > 1) {
            int additionalCommits = commitCount - 1;
            message.append(" + ").append(additionalCommits);
            message.append((additionalCommits >= 2) ? " more commits" : " more commit");
        }
        message.append(" — ").append(Web.getBitlyURL(payload.getCompare()));

        return message.toString();
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


