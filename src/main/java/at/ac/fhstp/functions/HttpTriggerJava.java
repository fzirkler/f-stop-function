package at.ac.fhstp.functions;

import java.util.*;
import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.*;
import com.azure.communication.email.models.EmailAddress;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import com.azure.resourcemanager.AzureResourceManager;

/**
 * Azure Functions with HTTP Trigger.
 */
public class HttpTriggerJava {
    /**
     * This function listens at endpoint "/api/HttpTriggerJava". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpTriggerJava
     * 2. curl {your host}/api/HttpTriggerJava?name=HTTP%20Query
     */
    @FunctionName("StopResource")
    public HttpResponseMessage run(
            @HttpTrigger(name = "resourceId", methods = {HttpMethod.GET}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        String query = request.getQueryParameters().get("resourceId");
        String resourceId = request.getBody().orElse(query);
        context.getLogger().info("resourceId: " + resourceId);

        if (resourceId == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass a resourceId on the query string or in the request body").build();
        } else {
            // Authentifizierung über Managed Identity
            ManagedIdentityCredential credential = new ManagedIdentityCredentialBuilder().build();

            // Azure-Profil mit Umgebung und Subscription
            AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);

            // AzureResourceManager-Client erstellen
            AzureResourceManager azure = AzureResourceManager
                    .authenticate(credential, profile)
                    .withDefaultSubscription();

            return request.createResponseBuilder(HttpStatus.OK).body(stopResourceById(resourceId, azure)).build();
        }
    }

    public String stopResourceById(String resourceId, AzureResourceManager azure) {
        VirtualMachine vm = azure.virtualMachines().getById(resourceId);
        if (vm != null) {
            vm.powerOff();
            sendEmail(vm.name());
            return("VM wird gestoppt: " + vm.name());
        } else {
            return("VM nicht gefunden.");
        }

    }

    private void sendEmail(String vmName) {
        String connectionString = System.getenv("EMAIL_CONNECTION_STRING");
        String address = System.getenv("EMAIL_ADDRESS");

        EmailClient emailClient = new EmailClientBuilder().connectionString(connectionString).buildClient();
        EmailAddress toAddress = new EmailAddress(System.getenv("EMAIL_TO_ADDRESS"));

        EmailMessage emailMessage = new EmailMessage()
                .setSenderAddress(address)
                .setToRecipients(toAddress)
                .setSubject("Eine virtuelle Maschine wurde gestoppt!")
                .setBodyPlainText("Die virtuelle Maschine " + vmName + " wurde gestoppt!");

        SyncPoller<EmailSendResult, EmailSendResult> poller = emailClient.beginSend(emailMessage, null);
        PollResponse<EmailSendResult> result = poller.waitForCompletion();
    }
}




