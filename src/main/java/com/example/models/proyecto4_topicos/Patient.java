package com.example.models.proyecto4_topicos;

import com.example.models.Model;

import java.util.List;
import java.util.Date;

public class Patient extends Model {

    public Patient() {
        super("proyecto4_topicos.patients");
    }

    public static Patient fromModel(Model model) {
        if (model == null) return null;
        Patient m = new Patient();
        
		m.set("entry", model.get("entry"));
		m.set("exit", model.get("exit"));
		m.set("human_id", model.get("human_id"));
		m.set("id", model.get("id"));
		m.set("medical_condition", model.get("medical_condition"));
		m.set("medical_details", model.get("medical_details"));
        return m;
    }

    public static Patient find(Integer id) {
        return fromModel(Model.findFirst("proyecto4_topicos.patients", "id", id));
    }

    public static List<Patient> find(Patient m) {
        return Model
                .find("proyecto4_topicos.patients", m.getData())
                .stream()
                .map(Patient::fromModel)
                .toList();
    }

    public static List<Patient> all() {
        return Model
                .all("proyecto4_topicos.patients")
                .stream()
                .map(Patient::fromModel)
                .toList();
    }

    public static List<String> getColumnNames() {
        return List.of("entry", "exit", "human_id", "id", "medical_condition", "medical_details");
    }

    

	public Date getEntry() {
		return (Date) super.get("entry");
	}
	public void setEntry(Date Entry) {
		super.set("entry", Entry);
	}

	public Date getExit() {
		return (Date) super.get("exit");
	}
	public void setExit(Date Exit) {
		super.set("exit", Exit);
	}

	public Integer getHumanId() {
		return (Integer) super.get("human_id");
	}
	public void setHumanId(Integer HumanId) {
		super.set("human_id", HumanId);
	}

	public Integer getId() {
		return (Integer) super.get("id");
	}
	public void setId(Integer Id) {
		super.set("id", Id);
	}

	public String getMedicalCondition() {
		return (String) super.get("medical_condition");
	}
	public void setMedicalCondition(String MedicalCondition) {
		super.set("medical_condition", MedicalCondition);
	}

	public String getMedicalDetails() {
		return (String) super.get("medical_details");
	}
	public void setMedicalDetails(String MedicalDetails) {
		super.set("medical_details", MedicalDetails);
	}
}
