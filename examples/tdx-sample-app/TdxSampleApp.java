/*
 *   Copyright (c) 2023-2024 Intel Corporation
 *   All rights reserved.
 *   SPDX-License-Identifier: BSD-3-Clause
 */

// Java Collections Imports
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// Nimbus JOSE + JWT Library Import
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jose.util.Base64;

// Third-party Library Imports
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

// trust_authority_client imports
import com.intel.trustauthority.connector.*;
import com.intel.trustauthority.tdx.TdxAdapter;

/**
 * TdxSampleApp class, a sample application demonstrating TDX Quote collection/verification
 * from TDX enabled platform
 */
public class TdxSampleApp {

    // Logger object
    private static final Logger logger = LogManager.getLogger(TdxSampleApp.class);

    public static void main(String[] args) {
        try {
            // Set log level
            setLogLevel("TdxSampleApp");

            // For testing
            byte[] bytes = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09};

            // Create the TdxAdapter object
            TdxAdapter tdx_adapter = new TdxAdapter(bytes);

            // Fetch the Tdx Quote
            Evidence tdx_evidence = tdx_adapter.collectEvidence(bytes);

            // Convert TDX quote from bytes to Base64
            String base64Quote = Base64.encode(tdx_evidence.getEvidence()).toString();

            // Print the TDX quote in Base64 format
            logger.debug("TDX quote Base64 Encoded: " + base64Quote);

            // Convert TDX UserData from bytes to Base64
            String base64UserData = Base64.encode(tdx_evidence.getUserData()).toString();

            // Print the TDX UserData in Base64 format
            logger.debug("TDX user data Base64 Encoded: " + base64UserData);

            // Initialize Sample App variables
            String[] trust_authority_variables = init();
            String trustauthority_base_url = trust_authority_variables[0];
            String trustauthority_api_url = trust_authority_variables[1];
            String trustauthority_api_key = trust_authority_variables[2];
            String trustauthority_request_id = trust_authority_variables[3];

            // Initialize config required for connector using trustauthority_base_url, trustauthority_api_url and trustauthority_api_key
            Config cfg = new Config(trustauthority_base_url, trustauthority_api_url, trustauthority_api_key);
    
            // Initializing connector with the config
            TrustAuthorityConnector connector = new TrustAuthorityConnector(cfg);

            // Verifying attestation for TDX platform
            AttestArgs attestArgs = new AttestArgs(tdx_adapter, null, trustauthority_request_id);
            AttestResponse response = connector.attest(attestArgs);

            // Print the Request ID of token fetched from Trust Authority
            if (response.getHeaders().containsKey("request-id")) {
                // Print Request ID of fetched token
                logger.info("Request ID of fetched token: " + response.getHeaders().get("request-id"));
            }

            // Print the Trace ID of token fetched from Trust Authority
            logger.info("Trace ID of fetched token: " + response.getHeaders().get("trace-id"));

            // Print the Token fetched from Trust Authority
            logger.info("Token fetched from Trust Authority: " + response.getToken());

            // Verify the received token
            JWTClaimsSet claims = connector.verifyToken(response.getToken());
        } catch (Exception e) {
            logger.error("Exception: " + e);
        }
    }

    /**
     * Helper function to set log level
     * 
     * @param loggerName Class name of the log level to be set for
     */
    private static void setLogLevel(String loggerName) {
        // Fetch the log level from an environment variable
        String logLevel = System.getenv("LOG_LEVEL");
        if (logLevel == null) {
            logger.info("LOG_LEVEL environment variable not set. Using default log level: INFO");
            logLevel = "info";
        }

        // Set of strings to compare against
        String[] logLevels = {"info", "trace", "debug", "warn", "error", "fatal"};

        // Check if the targetString is not equal to any of the logLevels
        boolean notEqual = Arrays.stream(logLevels).noneMatch(logLevel.toLowerCase()::equals);
        if (notEqual) {
            logger.info("Invalid LOG_LEVEL set. Using default log level: INFO");
            logLevel = "info";
        }

        // Set log level based on environment variable
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configurator.setLevel(loggerName, org.apache.logging.log4j.Level.valueOf(logLevel));
        ctx.updateLoggers();
    }

    /**
     * Helper function to initialize the Sample App
     * 
     * @return String[] object containing the trust authority variables
     */
    private static String[] init() {
        // Fetch proxy settings from environment
        String httpsHost = System.getenv("HTTPS_PROXY_HOST");
        if (httpsHost == null) {
            logger.warn("HTTPS_PROXY_HOST is not set.");
        } else {
            // Setting proxy settings host
            System.setProperty("https.proxyHost", httpsHost);
        }
        String httpsPort = System.getenv("HTTPS_PROXY_PORT");
        if (httpsPort == null) {
            logger.warn("HTTPS_PROXY_PORT is not set.");
        } else {
            // Setting proxy settings host
            System.setProperty("https.proxyPort", httpsPort);
        }
        logger.debug("HTTPS_PROXY_HOST: " + httpsHost + ", HTTPS_PROXY_PORT: " + httpsPort);

        // Fetch TRUSTAUTHORITY_BASE_URL, TRUSTAUTHORITY_API_URL and TRUSTAUTHORITY_API_KEY from environment
        String trustauthority_base_url = System.getenv("TRUSTAUTHORITY_BASE_URL");
        if (trustauthority_base_url == null) {
            logger.error("TRUSTAUTHORITY_BASE_URL is not set.");
        }
        String trustauthority_api_url = System.getenv("TRUSTAUTHORITY_API_URL");
        if (trustauthority_api_url == null) {
            logger.error("TRUSTAUTHORITY_API_URL is not set.");
        }
        String trustauthority_api_key = System.getenv("TRUSTAUTHORITY_API_KEY");
        if (trustauthority_api_key == null) {
            logger.error("TRUSTAUTHORITY_API_KEY is not set.");
        }
        String trustauthority_request_id = System.getenv("TRUSTAUTHORITY_REQUEST_ID");
        if (trustauthority_request_id == null) {
            logger.error("TRUSTAUTHORITY_REQUEST_ID is not set.");
        }
        logger.debug("TRUSTAUTHORITY_BASE_URL: " + trustauthority_base_url + ", TRUSTAUTHORITY_API_URL: " + trustauthority_api_url + ", TRUSTAUTHORITY_API_KEY: " + trustauthority_api_key);
        
        // Initialize trust authority variables
        String[] initializer = new String[4];
        initializer[0] = trustauthority_base_url;
        initializer[1] = trustauthority_api_url;
        initializer[2] = trustauthority_api_key;
        initializer[3] = trustauthority_request_id;

        return initializer;
    }
}