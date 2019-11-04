package com.test.vm;

import java.io.File;
import java.util.Arrays;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.KnownLinuxVirtualMachineImage;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.graphrbac.BuiltInRole;
import com.microsoft.azure.management.msi.Identity;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.rest.LogLevel;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) throws Exception {
        final File credFile = new File(System.getenv("AZURE_AUTH_LOCATION"));

        Azure azure = Azure.configure()
                .withLogLevel(LogLevel.BODY)
                .authenticate(credFile)
                .withDefaultSubscription();

        final String rgName = "rg-lskjlkredgswertyt";
        final String idName = "id-sklejrlksdjwertyh";
        final String vmName = "vm-slkejrijiodersfxv";
        final String pipName = "pip-slekjrlksjelrfs";
        final String userName = "lweroiuodglrnt";
        final String password = "NewPa$5w0rd!";
        final Region region = Region.ASIA_EAST;

        try {
            azure.resourceGroups().define(rgName)
                .withRegion(region)
                .create();

            Identity id = azure.identities()
                .define(idName)
                .withRegion(region)
                .withExistingResourceGroup(rgName)
                .withAccessToCurrentResourceGroup(BuiltInRole.CONTRIBUTOR)
                .create();

            VirtualMachine vm = azure.virtualMachines()
                .define(vmName)
                .withRegion(region)
                .withExistingResourceGroup(rgName)
                .withNewPrimaryNetwork("10.0.0.0/8")
                .withPrimaryPrivateIPAddressDynamic()
                .withNewPrimaryPublicIPAddress(pipName)
                .withPopularLinuxImage(KnownLinuxVirtualMachineImage.UBUNTU_SERVER_16_04_LTS)
                .withRootUsername(userName)
                .withRootPassword(password)
                .withExistingUserAssignedManagedServiceIdentity(id)
                .withSystemAssignedManagedServiceIdentity()
                .withSystemAssignedIdentityBasedAccessToCurrentResourceGroup(BuiltInRole.CONTRIBUTOR)
                .create();

            String[] scripts = {
                "sudo apt-get update",
                "sudo apt-get install -y openjdk-8-jdk maven git",
                "git clone https://github.com/Azure-Samples/compute-java-manage-vm-from-vm-with-msi-credentials.git",
                "cd compute-java-manage-vm-from-vm-with-msi-credentials",
                String.format("mvn clean compile exec:java -Dexec.args='%s %s %s' -Dexec.cleanupDaemonThreads=false", azure.subscriptionId(), rgName, id.clientId()),
                String.format("mvn clean compile exec:java -Dexec.args='%s %s' -Dexec.cleanupDaemonThreads=false", azure.subscriptionId(), rgName)
            };

            vm.runShellScript(Arrays.asList(scripts), null);
            
            PagedList<VirtualMachine> virtualMachines = azure.virtualMachines().listByResourceGroup(rgName);
            for (VirtualMachine virtualMachine : virtualMachines) {
                System.out.println(virtualMachine.id());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            azure.resourceGroups().deleteByName(rgName);
        }
    }
}