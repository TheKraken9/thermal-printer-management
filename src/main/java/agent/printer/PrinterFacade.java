package agent.printer;

import agent.dto.ReceiptDTO;

public class PrinterFacade {

    public static void print(ReceiptDTO receipt) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("linux")) {
            new LinuxPrinter().print(receipt);
        } else if (os.contains("win")) {
            new WindowsPrinter().print(receipt);
        } else {
            throw new RuntimeException("OS non supporté: " + os);
        }
    }
}
