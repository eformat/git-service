package org.acme;

import io.vertx.core.json.JsonObject;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.security.SecureRandom;

@Path("/v1/api")
public class GenerateResource {

    private final Logger log = LoggerFactory.getLogger(GenerateResource.class);

    @ConfigProperty(name = "git.username")
    public String gitUser;

    @ConfigProperty(name = "git.password")
    public String gitPassword;

    @ConfigProperty(name = "git.url")
    public String gitRepo;

    @POST
    @Path("generate")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public String generate(JsonObject specification) {
        log.info("{}", specification);
        try {
            createGitRepo(specification);
        } catch (Exception e) {
            e.printStackTrace();
            return "failed";
        }
        return "done";
    }

    protected void createGitRepo(JsonObject specification) throws IOException, GitAPIException, URISyntaxException {
        File localPath = File.createTempFile("tempRepo", "");
        Files.delete(localPath.toPath());

        CredentialsProvider cp = new UsernamePasswordCredentialsProvider(gitUser, gitPassword);
        try {
            SystemReader.getInstance().getUserConfig().clear();
        } catch (ConfigInvalidException e) {
            log.warn(e.getMessage());
        }

        try (Git git = Git.cloneRepository()
                .setURI(gitRepo)
                .setDirectory(localPath)
                .setCredentialsProvider(cp)
                .call()) {

            log.info("Cloned master repo: {}", git.getRepository().getDirectory());

            final String branchName = "api-spec-" + getAlphaNumericString(4);
            log.info("Git Branch URL: {}", gitRepo.replaceAll(".git", "") + "/src/" + branchName);

            git.checkout()
                    .setCreateBranch(true)
                    .setName(branchName)
                    .call();

            final StoredConfig config = git.getRepository().getConfig();
            RemoteConfig remoteConfig = new RemoteConfig(config, "gogs");
            URIish uri = new URIish(git.getRepository().getDirectory().toURI()
                    .toURL());
            remoteConfig.addURI(uri);
            remoteConfig.addPushRefSpec(new RefSpec("HEAD:refs/heads/" + branchName));
            remoteConfig.update(config);
            config.save();

            File myFile = new File(git.getRepository().getDirectory().getParent(), "openapi-spec.json");
            FileWriter fileWriter = new FileWriter(myFile);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            try {
                printWriter.print(specification);
            } finally {
                printWriter.close();
                fileWriter.close();
            }

            git.add().addFilepattern("openapi-spec.json").call();
            RevCommit commit = git.commit().setMessage("\uD83E\uDDA9 Initial commit \uD83E\uDDA9").call();
            log.info("Committed: {}", commit.getId());
            git.push().setCredentialsProvider(cp).setRemote("origin").call();
            log.info("Pushed: {}", commit.getId());
        }
        FileUtils.deleteDirectory(localPath);
    }

    private String getAlphaNumericString(int n) {
        String alpha = "0123456789"
                + "abcdefghijklmnopqrstuvxyz";
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            // generate a random number between
            // 0 to alpha variable length
            int index
                    = (int) (alpha.length()
                    * Math.random());
            sb.append(alpha
                    .charAt(index));
        }
        return sb.toString();
    }
}
