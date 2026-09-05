import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { StudentService } from '../service/student.service';
import { Student } from '../model/student';

@Component({
  selector: 'app-student-form',
  templateUrl: './student-form.component.html',
  styleUrls: ['./student-form.component.css']
})
export class StudentFormComponent {

  student: Student;

  /** Meldungen aus der Backend-Validierung (Feld -> Text). */
  serverFehler: string[] = [];

  constructor(private route: ActivatedRoute, private router: Router, private studentService: StudentService) {
    this.student = new Student();
  }

  onSubmit() {
    this.serverFehler = [];

    this.studentService.save(this.student).subscribe({
      next: () => this.gotoStudentList(),
      error: (fehler) => {
        // Das Backend liefert bei 400 ein Objekt { fields: { name: "...", ... } }
        const fields = fehler?.error?.fields;
        this.serverFehler = fields
          ? Object.values(fields) as string[]
          : ['Speichern fehlgeschlagen. Bitte spaeter erneut versuchen.'];
      }
    });
  }

  gotoStudentList() {
    this.router.navigate(['/students']);
  }
}
