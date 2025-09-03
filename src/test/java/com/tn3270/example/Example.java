package com.tn3270.example;

import com.tn3270.Tn3270;
import com.tn3270.Screen;
import com.tn3270.TerminalType;
import java.io.IOException;

public class Example {
    
    public static void main(String[] args) {
        headlessExample();
    }
    
    public static void headlessExample() {
        Tn3270 tn3270 = new Tn3270()
            .setHost("mainframe.example.com")
            .setPort(23)
            .setSslPort(992)
            .setTerminalType(TerminalType.IBM_3278_2_E)
            .setConnectTimeout(10000)
            .setReadTimeout(30000);
        
        try {
            tn3270.connect();
            
            Screen screen = tn3270.getScreen();
            
            screen.waitForUnlock();
            
            screen.waitForText("USERID");
            
            screen
                .putString("USER123")
                .tab()
                .putString("PASSWORD")
                .enter();
            
            screen.waitForUnlock();
            
            screen
                .putString("LOGON APPLID(TSO)")
                .enter();
            
            screen.waitForText("***");
            
            screen
                .clear()
                .putString("ISPF")
                .enter();
            
            screen.waitForUnlock();
            
            screen.pf3();
            
            screen.waitForUnlock();
            
            String screenContent = screen.getText();
            System.out.println("Current screen content:");
            System.out.println(screenContent);
            
            String[] lines = screen.getLines();
            for (int i = 0; i < lines.length; i++) {
                System.out.println(String.format("%02d: %s", i + 1, lines[i]));
            }
            
            String fieldValue = screen.getFieldString(2);
            System.out.println("Field 2 value: " + fieldValue);
            
            screen
                .setCursor(10, 20)
                .putString("Hello World")
                .home()
                .eraseEOF()
                .enter();
            
        } catch (IOException e) {
            System.err.println("Connection failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            tn3270.disconnect();
        }
    }
    
    public static void automatedWorkflowExample() {
        Tn3270 tn3270 = new Tn3270("mainframe.example.com");
        
        try {
            tn3270.connect();
            Screen screen = tn3270.getScreen();
            
            screen
                .waitForText("READY")
                .putString("LOGON TSO/USER123")
                .enter()
                .waitForText("PASSWORD")
                .putString("PASS123")
                .enter()
                .waitForUnlock()
                .putString("ALLOC DD(TEMP) NEW SPACE(1,1) TRACKS")
                .enter()
                .waitForUnlock()
                .putString("LISTCAT")
                .enter()
                .waitForUnlock();
            
            String catalogListing = screen.getText();
            processCatalogListing(catalogListing);
            
            screen
                .clear()
                .putString("LOGOFF")
                .enter();
                
        } catch (IOException e) {
            System.err.println("Workflow failed: " + e.getMessage());
        } finally {
            tn3270.disconnect();
        }
    }
    
    private static void processCatalogListing(String listing) {
        System.out.println("Processing catalog listing...");
        String[] lines = listing.split("\n");
        for (String line : lines) {
            if (line.contains("VSAM")) {
                System.out.println("Found VSAM dataset: " + line.trim());
            }
        }
    }
}