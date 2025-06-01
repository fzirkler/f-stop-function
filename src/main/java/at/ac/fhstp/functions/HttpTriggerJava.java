package at.ac.fhstp.functions;

import java.util.*;

import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.GenericResource;

/**
 * Azure Functions with HTTP Trigger.
 */
public class HttpTriggerJava {
    /**
     * This function listens at endpoint "/api/HttpTriggerJava". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpTriggerJava
     * 2. curl {your host}/api/HttpTriggerJava?name=HTTP%20Query
     */
    @FunctionName("DeleteResource")
    public HttpResponseMessage run(
            @HttpTrigger(name = "resourceId", methods = {HttpMethod.GET}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
        String query = request.getQueryParameters().get("name");
        String resourceId = request.getBody().orElse(query);

        if (resourceId == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass a ressourceId on the query string or in the request body").build();
        } else {
            return request.createResponseBuilder(HttpStatus.OK).body(deleteResourceById(resourceId)).build();
        }
    }




    public String deleteResourceById(String resourceId) {
        // Authentifizierung über Managed Identity
        ManagedIdentityCredential credential = new ManagedIdentityCredentialBuilder().build();

        // Azure-Profil mit Umgebung und Subscription
        AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);

        // AzureResourceManager-Client erstellen
        AzureResourceManager azure = AzureResourceManager
                .authenticate(credential, profile)
                .withDefaultSubscription();

        // Ressource abrufen und löschen

        VirtualMachine vm = azure.virtualMachines().getById(resourceId);
        if (vm != null) {
            vm.powerOff(); // oder vm.deallocate() für vollständiges Freigeben der Ressourcen
            return("VM wird gestoppt: " + vm.name());
        } else {
            return("VM nicht gefunden.");
        }

    }

}




