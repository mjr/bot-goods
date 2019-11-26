package entities;

import util.Util;

import java.io.*;

public abstract class Common {
    private int code;
    private String name;
    private String description;

    public Common() {}

    public Common(int code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String objectToString() {
        return this.getCode() + Util.SEPARATOR + this.getName() + Util.SEPARATOR + this.getDescription();
    }

    public boolean save() {
        try {
            FileWriter fileWriter = new FileWriter(
                new File(this.getClass().getSimpleName().toLowerCase() + ".txt"),
                true
            );
            fileWriter.write(this.objectToString() + "\n");
            fileWriter.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
            System.out.println(e);
            return false;
        } catch (IOException e) {
            System.out.println("Error initializing stream");
            System.out.println(e);
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "Código: " + this.getCode() + "\nNome: " + this.getName() + "\nDescrição: " + this.getDescription();
    }
}
