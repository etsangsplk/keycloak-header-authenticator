package com.alliander.keycloak.authenticator;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.Response;

/**
 * Created by joris on 25/11/2016.
 */
public class KeycloakHeaderAuthenticator implements Authenticator {

    private static Logger logger = Logger.getLogger(KeycloakHeaderAuthenticator.class);

    public static final String CREDENTIAL_TYPE = "http-header_validation";

    public void authenticate(AuthenticationFlowContext context) {
        logger.debug("authenticate called ... context = " + context);

        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        String headerName = config.getConfig().get(HDRAuthenticatorContstants.CONF_PRP_HEADER_NAME);
        String requiredValue = config.getConfig().get(HDRAuthenticatorContstants.CONF_PRP_HEADER_REQ_VALUE);

        if(headerName == null || requiredValue == null) {
            Response challenge =  context.form()
                    .setError("HTTP Header validator is not configured")
                    .createForm("hdr-validation.ftl");
            logger.debug("Calling context.failureChallenge( ... )");

            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
            return;
        }

        String headerValue = null;

        try {
            headerValue = context.getHttpRequest().getHttpHeaders().getHeaderString(headerName);
        } catch (NullPointerException npe) {
            // ignore
        }

        logger.debug("Header " + headerName + " has value " + headerValue + " ( expected " +  requiredValue + ")");

        if(headerValue != null && headerValue.equals(requiredValue)) {
            // Ok, let the user in
            logger.debug("Calling context.success()");
            context.success();
        } else {
            if(context.getExecution().getRequirement() == AuthenticationExecutionModel.Requirement.OPTIONAL ||
                    context.getExecution().getRequirement() == AuthenticationExecutionModel.Requirement.ALTERNATIVE) {
                logger.debug("Calling context.attempted()");
                context.attempted();
            } else if(context.getExecution().getRequirement() == AuthenticationExecutionModel.Requirement.REQUIRED) {
                logger.debug("Calling context.failure()");
                context.failure(AuthenticationFlowError.INVALID_CREDENTIALS);
            } else {
                // Something strange happened
            }
        }
    }

    public void action(AuthenticationFlowContext context) {
        // Nothing here
    }

    public boolean requiresUser() {
        // We don't need a User to check if an HTTP header is passed or not
        return false;
    }

    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        // This method should not be called (because requiresUser returns false).
        return false;
    }

    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        logger.debug("setRequiredActions called ... session=" + session + ", realm=" + realm + ", user=" + user);
    }

    public void close() {
        logger.debug("close called ...");
    }


}
