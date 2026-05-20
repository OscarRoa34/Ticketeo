package co.edu.uptc.Ticketeo.utils;

import com.microsoft.playwright.*;

public class AutoFill {

    public static void main(String[] args) {
        
        // ==========================================
        // MENÚ DE CASOS (Comenta y descomenta el que quieras usar)
        // ==========================================
        
        // CASO 1: Logueo con oscar, tarjeta VISA válida y pagar
        ejecutarEscenario("oscar", "1", "VISA", "4532015112830366");

        // CASO 2: Logueo con oscar, tarjeta VISA inválida (puros 1) y pagar
        // ejecutarEscenario("oscar", "1", "VISA", "1111111111111111");
        
    }

    /**
     * Método reutilizable que ejecuta todo el flujo de compra.
     * Solo le pasas los datos que cambian entre cada caso.
     */
    public static void ejecutarEscenario(String usuario, String password, String marcaTarjeta, String numeroTarjeta) {
        
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(false).setSlowMo(100)
            );
            Page page = browser.newPage();

            // 1. LOGIN
            System.out.println("Ejecutando caso con tarjeta: " + numeroTarjeta);
            page.navigate("http://localhost:8080/login");
            page.locator("#username").fill(usuario);
            page.locator("#password").fill(password);
            page.locator(".login-btn").click();

            // 2. NAVEGAR A COMPRA
            page.navigate("http://localhost:8080/event/32/purchase");

            // 3. RELLENAR FORMULARIO
            // Seleccionar 2 boletos para habilitar el botón de pago
            Locator primerBoletoOption = page.locator(".ticket-qty-input").first();
            if (primerBoletoOption.count() > 0) {
                primerBoletoOption.fill("2"); 
            }

            // Seleccionar la marca de la tarjeta (dinámico)
            page.locator("input[name='cardBrandOption'][value='" + marcaTarjeta + "']").check();
            
            // Escribir el número de la tarjeta (dinámico)
            page.locator("#cardNumber").fill(numeroTarjeta);

            // 4. PAGAR
            System.out.println("Haciendo clic en Pagar...");
            page.locator("#payButton").click();

            // 5. PAUSA
            // Dejamos la pantalla abierta para que veas qué ocurrió (si pasó a la vista de éxito o si el HTML te mostró un error).
            page.pause(); 
        }
    }
}