package agent.dto;

public class ReceiptExtraLineDTO {
    public String label;
    public double quantity;
    public long   unitPrice;
    public long   totalAmount;
    public String executionDate;   // format "dd/MM/yyyy", null si absent
    public String note;
    public String paymentStatus;   // "pending" | "partial" | "completed"
    public long   amountPaid;
    public long   remainingAmount;
}