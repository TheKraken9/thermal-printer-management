package agent.printer;

import agent.dto.ReceiptDTO;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Impression sous Linux. Reutilise EXACTEMENT la mise en page de WindowsPrinter
 * (buildTicket) pour un rendu identique cote client. Ordre d'essai :
 *   1. peripherique USB thermique direct (/dev/usb/lp0, ...)
 *   2. imprimante par defaut du systeme (CUPS, via javax.print)
 *   3. aucun materiel (poste de developpement) : le ticket est ecrit dans un
 *      fichier + affiche en console, SANS lever d'exception, pour verifier le
 *      rendu sans imprimante branchee.
 */
public class LinuxPrinter {

    private static final String[] USB_DEVICES = { "/dev/usb/lp0", "/dev/usb/lp1", "/dev/lp0" };
    private static final byte[] CUT = new byte[]{ 0x1D, 0x56, 0x00 };

    public void print(ReceiptDTO r) throws Exception {
        // Mise en page commune (identique a Windows).
        String ticketText = new WindowsPrinter().buildTicket(r);

        // 1. Peripherique USB thermique direct.
        for (String dev : USB_DEVICES) {
            Path p = Paths.get(dev);
            if (Files.exists(p) && Files.isWritable(p)) {
                try (OutputStream os = Files.newOutputStream(p)) {
                    os.write(concatCut(ticketText));
                    os.flush();
                }
                System.out.println("Imprime sur " + dev);
                return;
            }
        }

        // 2. Imprimante par defaut du systeme (CUPS).
        PrintService service = PrintServiceLookup.lookupDefaultPrintService();
        if (service != null) {
            System.out.println("Imprimante : " + service.getName());
            WindowsPrinter.sendToPrinter(service, ticketText);
            return;
        }

        // 3. Poste de developpement sans imprimante : on ne plante pas.
        Path out = Paths.get(System.getProperty("java.io.tmpdir"),
                "ticket-" + System.currentTimeMillis() + ".txt");
        Files.write(out, ticketText.getBytes(StandardCharsets.UTF_8));
        System.out.println("Aucune imprimante detectee (poste de dev). Ticket ecrit dans : " + out);
    }

    private static byte[] concatCut(String text) {
        byte[] t = text.getBytes(StandardCharsets.US_ASCII);
        byte[] d = new byte[t.length + CUT.length];
        System.arraycopy(t, 0, d, 0, t.length);
        System.arraycopy(CUT, 0, d, t.length, CUT.length);
        return d;
    }
}
