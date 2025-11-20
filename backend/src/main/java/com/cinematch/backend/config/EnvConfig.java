package com.cinematch.backend.config;

// Φορτώνουμε τη βιβλιοθήκη dotenv για να διαβάζουμε μεταβλητές από το .env
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// Δηλώνουμε ότι αυτή η κλάση περιέχει Spring Beans
public class EnvConfig {

    @Bean
    // Δηλώνουμε ότι αυτή η μέθοδος δημιουργεί ένα αντικείμενο Dotenv
    // το οποίο μπορεί να το κάνει inject το Spring σε άλλες κλάσεις.
    public Dotenv dotenv() {

        // Configure() ξεκινάει τη δημιουργία του loader για το .env
        return Dotenv.configure()
                .ignoreIfMalformed()   // Αν το .env έχει λάθος syntax, μην πετάξεις exception
                .ignoreIfMissing()     // Αν λείπει το .env στο production, μην κρασάρεις
                .load();               // Φόρτωσε πραγματικά το .env και διάβασε τις μεταβλητές
    }
}

