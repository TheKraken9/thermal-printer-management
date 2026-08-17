package agent.printer;

import agent.dto.ReceiptDTO;

public class PrinterFacade {

    public static void print(ReceiptDTO receipt) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            new WindowsPrinter().print(receipt);
        } else {
            // Linux (postes de dev) et macOS : meme mecanisme (USB direct / CUPS),
            // avec repli fichier si aucune imprimante n'est branchee.
            new LinuxPrinter().print(receipt);
        }
    }
}
