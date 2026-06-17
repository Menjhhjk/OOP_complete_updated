module com.iskollect.oop_complete_project {
    requires jbcrypt;
    requires javafx.controls;
    requires javafx.fxml;
    requires com.dlsc.formsfx;
    requires java.sql;
    requires org.postgresql.jdbc;
    opens com.iskollect to javafx.fxml;
    opens com.iskollect.controller to javafx.fxml;
    exports com.iskollect;
}
