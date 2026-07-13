module com.learn.registration {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    // SQLite JDBC driver loaded reflectively at runtime
    requires org.xerial.sqlitejdbc;

    // BCrypt for password hashing
    requires jbcrypt;

    // FXML needs reflective access to our controller classes
    opens com.learn.registration             to javafx.fxml;
    opens com.learn.registration.view       to javafx.fxml;
    opens com.learn.registration.controller to javafx.fxml;
    opens com.learn.registration.model      to javafx.fxml, javafx.base;
    opens com.learn.registration.database   to javafx.fxml;
    opens com.learn.registration.security   to javafx.fxml;

    exports com.learn.registration;
    exports com.learn.registration.view;
    exports com.learn.registration.controller;
    exports com.learn.registration.model;
    exports com.learn.registration.database;
    exports com.learn.registration.security;
}
