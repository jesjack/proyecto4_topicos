package com.example.models.proyecto4_topicos;

import com.example.models.Model;

import java.util.List;
import java.util.Date;

public class Human extends Model {

    public Human() {
        super("proyecto4_topicos.humans");
    }

    public static Human fromModel(Model model) {
        if (model == null) return null;
        Human m = new Human();
        
		m.set("birthdate", model.get("birthdate"));
		m.set("curp", model.get("curp"));
		m.set("firstname", model.get("firstname"));
		m.set("gender", model.get("gender"));
		m.set("id", model.get("id"));
		m.set("lastname", model.get("lastname"));
        return m;
    }

    public static Human find(Integer id) {
        return fromModel(Model.findFirst("proyecto4_topicos.humans", "id", id));
    }

    public static List<Human> find(Human m) {
        return Model
                .find("proyecto4_topicos.humans", m.getData())
                .stream()
                .map(Human::fromModel)
                .toList();
    }

    public static List<Human> all() {
        return Model
                .all("proyecto4_topicos.humans")
                .stream()
                .map(Human::fromModel)
                .toList();
    }

    public static List<String> getColumnNames() {
        return List.of("birthdate", "curp", "firstname", "gender", "id", "lastname");
    }

    

	public Date getBirthdate() {
		return (Date) super.get("birthdate");
	}
	public void setBirthdate(Date Birthdate) {
		super.set("birthdate", Birthdate);
	}

	public String getCurp() {
		return (String) super.get("curp");
	}
	public void setCurp(String Curp) {
		super.set("curp", Curp);
	}

	public String getFirstname() {
		return (String) super.get("firstname");
	}
	public void setFirstname(String Firstname) {
		super.set("firstname", Firstname);
	}

	public Object getGender() {
		return (Object) super.get("gender");
	}
	public void setGender(Object Gender) {
		super.set("gender", Gender);
	}

	public Integer getId() {
		return (Integer) super.get("id");
	}
	public void setId(Integer Id) {
		super.set("id", Id);
	}

	public String getLastname() {
		return (String) super.get("lastname");
	}
	public void setLastname(String Lastname) {
		super.set("lastname", Lastname);
	}
}
