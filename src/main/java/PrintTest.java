import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.escpos.EscPosConst;

import java.io.FileOutputStream;
import java.io.OutputStream;

public class PrintTest {

    public static void main(String[] args) throws Exception {

        // 🔌 Sortie USB brute (Linux)
        OutputStream os = new FileOutputStream("/dev/usb/lp0");

        EscPos escpos = new EscPos(os);

        Style title = new Style()
                .setBold(true)
                .setFontSize(Style.FontSize._2, Style.FontSize._2)
                .setJustification(EscPosConst.Justification.Center);

        escpos.writeLF(title, "RIDEAUX & DECO");
        escpos.writeLF("--------------------------------");
        escpos.writeLF("TEST POS 8360");
        escpos.writeLF("--------------------------------");
        escpos.writeLF("Rideau Bleu    2 x 20000");
        escpos.writeLF("Voile Blanc    1 x 10000");
        escpos.writeLF("--------------------------------");
        escpos.writeLF("TOTAL : 50000 Ar");
        escpos.writeLF("--------------------------------");
        escpos.writeLF("MERCI POUR VOTRE VISITE");

        escpos.feed(4);
        escpos.cut(EscPos.CutMode.FULL);

        escpos.close();
        os.close();
    }
}
