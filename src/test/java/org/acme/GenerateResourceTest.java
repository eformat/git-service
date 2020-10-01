package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.JsonObject;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.common.Assert;

import javax.inject.Inject;

import static org.mockito.Mockito.*;

@QuarkusTest
@DisplayName("GenerateResource")
public class GenerateResourceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger("JGitRepositoryTest");

    @Inject
    GenerateResource service;

    @Test
    public void testGitServiceEndpoint() {
        Assertions.assertEquals("done", service.generate(new JsonObject()));
        Git git = mock(Git.class);
        Ref ref = mock(Ref.class);
        doNothing().when(git).close();
        CheckoutCommand mockCheckoutCommand = mock(CheckoutCommand.class);
        when(mockCheckoutCommand.setCreateBranch(anyBoolean())).thenReturn(mockCheckoutCommand);
        when(mockCheckoutCommand.setName(anyString())).thenReturn(mockCheckoutCommand);
        when(git.checkout()).thenReturn(mockCheckoutCommand);
        try {
            when(mockCheckoutCommand.call()).thenReturn(ref);
            service.git = git;
            service.createGitRepo(new JsonObject());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void gitTest() throws GitAPIException {
        Git git = mock(Git.class);
        Ref ref = mock(Ref.class);
        doNothing().when(git).close();
        CheckoutCommand mockCheckoutCommand = mock(CheckoutCommand.class);
        when(mockCheckoutCommand.setCreateBranch(anyBoolean())).thenReturn(mockCheckoutCommand);
        when(mockCheckoutCommand.setName(anyString())).thenReturn(mockCheckoutCommand);
        when(mockCheckoutCommand.call()).thenReturn(ref);
        when(git.checkout()).thenReturn(mockCheckoutCommand);
        try {
            git.checkout()
                    .setCreateBranch(true)
                    .setName("test")
                    .call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetAlphaNumericString () {
        final String ret = service.getAlphaNumericString(4);
        Assert.assertTrue(ret.length() == 4);
    }
}
