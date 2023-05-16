package com.example.models.proyecto4_topicos;

import com.example.models.Model;

import java.util.List;
import java.util.Date;

public class Doctor extends Model {

    public Doctor() {
        super("proyecto4_topicos.doctors");
    }

    public static Doctor fromModel(Model model) {
        if (model == null) return null;
        Doctor m = new Doctor();
        
		m.set("department", model.get("department"));
		m.set("human_id", model.get("human_id"));
		m.set("id", model.get("id"));
		m.set("professional_license", model.get("professional_license"));
        return m;
    }

    public static Doctor find(Integer id) {
        return fromModel(Model.findFirst("proyecto4_topicos.doctors", "id", id));
    }

    public static List<Doctor> find(Doctor m) {
        return Model
                .find("proyecto4_topicos.doctors", m.getData())
                .stream()
                .map(Doctor::fromModel)
                .toList();
    }

    public static List<Doctor> all() {
        return Model
                .all("proyecto4_topicos.doctors")
                .stream()
                .map(Doctor::fromModel)
                .toList();
    }

    public static List<String> getColumnNames() {
        return List.of("department", "human_id", "id", "professional_license");
    }

    

	public String getDepartment() {
		return (String) super.get("department");
	}
	public void setDepartment(String Department) {
		super.set("department", Department);
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

	public Object getProfessionalLicense() {
		return (Object) super.get("professional_license");
	}
	public void setProfessionalLicense(Object ProfessionalLicense) {
		super.set("professional_license", ProfessionalLicense);
	}
}
