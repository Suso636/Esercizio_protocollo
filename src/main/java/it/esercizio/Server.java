package it.esercizio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Server {
    private static final int PORTA = 3000;

    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(PORTA)) {
            System.out.println("Server in ascolto sulla porta " + PORTA);

            while (true) {
                // Una connessione alla volta; il server poi torna ad accept().
                try (Socket socket = server.accept()) {
                    gestisciConnessione(socket);
                } catch (IOException e) {
                    System.err.println("Errore nella connessione: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Impossibile avviare il server: " + e.getMessage());
        }
    }

    private static void gestisciConnessione(Socket socket) throws IOException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

        PrintWriter out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                true);

        String richiesta = in.readLine();

        // Il client può collegarsi e chiudere senza inviare una richiesta.
        if (richiesta == null) {
            return;
        }

        System.out.println("Richiesta ricevuta: " + richiesta);

        String pulita = richiesta.trim();

        if (pulita.isEmpty()) {
            inviaErrore(out, "ERR SYNTAX");
            return;
        }

        String[] parti = pulita.split("\\s+");

        if (!parti[0].equals("LEGGI")) {
            inviaErrore(out, "ERR UNKNOWNCMD");
            return;
        }

        if (parti.length != 2 || !nomeValido(parti[1])) {
            inviaErrore(out, "ERR SYNTAX");
            return;
        }

        File cartella = new File("documenti");
        File file = new File(cartella, parti[1]);

        // Messaggio utile per controllare dove il server cerca il file.
        System.out.println("Cerco il file qui: " + file.getAbsolutePath());

        if (!file.isFile()) {
            inviaErrore(out, "ERR NOTFOUND");
            return;
        }

        BufferedReader lettoreFile;

        try {
            lettoreFile = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(file),
                            StandardCharsets.UTF_8));
        } catch (IOException e) {
            inviaErrore(out, "ERR READ");
            System.err.println("Impossibile aprire il file: " + e.getMessage());
            return;
        }

        try (BufferedReader lettore = lettoreFile) {
            out.println("OK FILE");
            out.println(); // Separa lo stato dal contenuto.

            String riga;
            while ((riga = lettore.readLine()) != null) {
                out.println(riga);
            }
        } catch (IOException e) {
            // OK FILE è già stato inviato: non mandare un altro stato.
            System.err.println("Errore leggendo il file: " + e.getMessage());
        }
    }

    private static boolean nomeValido(String nome) {
        return nome.endsWith(".txt")
                && !nome.contains(" ")
                && !nome.contains("/")
                && !nome.contains("\\")
                && !nome.contains("..");
    }

    private static void inviaErrore(PrintWriter out, String stato) {
        out.println(stato);
        out.println(); // Anche gli errori hanno una riga vuota dopo lo stato.
    }
}
