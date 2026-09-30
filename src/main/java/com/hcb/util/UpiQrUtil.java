package com.hcb.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class UpiQrUtil {

    public static String buildUpiUrl(String upiId, String payeeName, BigDecimal amount, String orderNumber) {
        try {
            String encodedName = URLEncoder.encode(payeeName, StandardCharsets.UTF_8);
            String encodedNote = URLEncoder.encode("Payment for Order " + orderNumber, StandardCharsets.UTF_8);
            String formattedAmount = String.format("%.2f", amount);

            return "upi://pay?pa=" + upiId +
                    "&pn=" + encodedName +
                    "&am=" + formattedAmount +
                    "&cu=INR" +
                    "&tn=" + encodedNote;
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct UPI payment URL", e);
        }
    }

    public static String generateQrBase64(String content, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            byte[] imageBytes = outputStream.toByteArray();

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR Code image", e);
        }
    }

    public static void writeQrToFile(String content, int width, int height, java.nio.file.Path targetPath) {
        try {
            if (targetPath.getParent() != null) {
                java.nio.file.Files.createDirectories(targetPath.getParent());
            }
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height);
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", targetPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write QR Code to file", e);
        }
    }

    public static void main(String[] args) {
        String dummyUpi = "upi://pay?pa=healthychocobytes@paytm&pn=Healthy+Choco+Bytes&cu=INR";
        writeQrToFile(dummyUpi, 300, 300, java.nio.file.Paths.get("src/main/resources/static/images/dummy_qr.png"));
        writeQrToFile(dummyUpi, 300, 300, java.nio.file.Paths.get("target/classes/static/images/dummy_qr.png"));
        System.out.println("Dummy QR generated successfully!");
    }
}
