package ch.tbz.m450.testing.tools.repository.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Entity
@AllArgsConstructor
@Data
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @NotBlank(message = "Name darf nicht leer sein")
    @Size(max = 100, message = "Name darf hoechstens 100 Zeichen lang sein")
    private final String name;

    @NotBlank(message = "E-Mail darf nicht leer sein")
    @Email(message = "E-Mail ist keine gueltige Adresse")
    private final String email;

    public Student(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public Student() {
        this.name = "";
        this.email = "";
    }
}
