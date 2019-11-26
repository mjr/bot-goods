package entities;

import util.Util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Good {
    private int code;
    private String name;
    private String description;
    private Location location;
    private GoodsCategory category;

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

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public GoodsCategory getCategory() {
        return category;
    }

    public void setCategory(GoodsCategory category) {
        this.category = category;
    }

    public String objectToString() {
        return code + Util.SEPARATOR + name + Util.SEPARATOR + description + Util.SEPARATOR + location.objectToString() + Util.SEPARATOR + category.objectToString();
    }

    public static Good stringToObject(String text) {
        String[] attr = text.split(Util.SEPARATOR_REGEX);
        Good good = new Good();
        good.code = Integer.parseInt(attr[0]);
        good.name = attr[1];
        good.description = attr[2];
        good.location = new Location(Integer.parseInt(attr[3]), attr[4], attr[5]);
        good.category = new GoodsCategory(Integer.parseInt(attr[6]), attr[7], attr[8]);
        return good;
    }

    public static List<Good> filterByLocation(int code) {
        List<Good> goods = Good.read();
        List<Good> result = new ArrayList<>();
        for (Good good : goods) {
            if (good.getLocation().getCode() == code) {
                result.add(good);
            }
        }
        return result;
    }

    public static List<Good> filterByCategory(int code) {
        List<Good> goods = Good.read();
        List<Good> result = new ArrayList<>();
        for (Good good : goods) {
            if (good.getCategory().getCode() == code) {
                result.add(good);
            }
        }
        return result;
    }

    public static List<Good> filterByLocation(String filter) {
        List<Good> goods = Good.read();
        List<Good> result = new ArrayList<>();
        for (Good good : goods) {
            if (good.getLocation().getName().equals(filter)) {
                result.add(good);
            }
        }
        return result;
    }

    public static List<Good> filterByName(String filter) {
        List<Good> goods = Good.read();
        List<Good> result = new ArrayList<>();
        for (Good good : goods) {
            if (good.getName().equals(filter)) {
                result.add(good);
            }
        }
        return result;
    }

    public static List<Good> filterByDescription(String filter) {
        List<Good> goods = Good.read();
        List<Good> result = new ArrayList<>();
        for (Good good : goods) {
            if (good.getDescription().equals(filter)) {
                result.add(good);
            }
        }
        return result;
    }

    public static Good getByCode(int code) {
        List<Good> elements = Good.read();
        Good result = null;
        for (Good element : elements) {
            if (element.getCode() == code) {
                result = element;
            }
        }
        return result;
    }

    public static Good getByName(String name) {
        List<Good> elements = Good.read();
        Good result = null;
        for (Good element : elements) {
            if (element.getName().equals(name)) {
                result = element;
            }
        }
        return result;
    }

    public static Good getByDescription(String description) {
        List<Good> elements = Good.read();
        Good result = null;
        for (Good element : elements) {
            if (element.getName().equals(description)) {
                result = element;
            }
        }
        return result;
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

    public static List<Good> read() {
        List<Good> list = new ArrayList<>();

        try {
            FileReader fileReader = new FileReader(
                new File(Good.class.getSimpleName().toLowerCase() + ".txt")
            );
            Scanner scanner = new Scanner(fileReader);

            while (scanner.hasNextLine()) {
                list.add(Good.stringToObject(scanner.nextLine()));
            }
            scanner.close();
            fileReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
            System.out.println(e);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return list;
    }

    public static void delete(int code) {
        try {
            File tempDB = new File(Good.class.getSimpleName().toLowerCase() + "_temp.txt");
            File db = new File(Good.class.getSimpleName().toLowerCase() + ".txt");


            BufferedReader br = new BufferedReader( new FileReader( db ) );
            BufferedWriter bw = new BufferedWriter( new FileWriter( tempDB ) );

            String line;
            while ((line = br.readLine()) != null) {
                String[] attr = line.split(Util.SEPARATOR_REGEX);
                if (Integer.parseInt(attr[0]) == code)
                    continue;

                bw.write(line);
                bw.flush();
                bw.newLine();
            }

            br.close();
            bw.close();

            db.delete();
            tempDB.renameTo(db);
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
            System.out.println(e);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "Código: " + code + "\nNome: " + name + "\nDescrição: " + description + "\nLocalização: " + location.getName() + "\nCategoria: " + category.getName();
    }
}
