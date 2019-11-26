package entities;

import util.Util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Location extends Common {
    public Location() {}
    public Location(int code, String name, String description){
        super(code, name, description);
    }

    public static Location stringToObject(String text) {
        String[] attr = text.split(Util.SEPARATOR_REGEX);
        Location element = new Location();
        element.setCode(Integer.parseInt(attr[0]));
        element.setName(attr[1]);
        element.setDescription(attr[2]);
        return element;
    }

    public static Location get(String filter) {
        List<Location> elements = Location.read();
        Location result = null;
        for (Location element : elements) {
            if (element.getName().equals(filter)) {
                result = element;
            }
        }
        return result;
    }

    public static Location getByCode(int code) {
        List<Location> elements = Location.read();
        Location result = null;
        for (Location element : elements) {
            if (element.getCode() == code) {
                result = element;
            }
        }
        return result;
    }

    public static void delete(int code) {
        try {
            File tempDB = new File(Location.class.getSimpleName().toLowerCase() + "_temp.txt");
            File db = new File(Location.class.getSimpleName().toLowerCase() + ".txt");


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

    public static List<Location> filterByNull() {
        List<Location> locations = Location.read();
        List<Location> result = new ArrayList<>();
        for (Location location : locations) {
            if (Good.filterByLocation(location.getCode()).isEmpty()) {
                result.add(location);
            }
        }

        return result;
    }

    public static List<Location> excludeByCode(int code) {
        List<Location> locations = Location.read();
        List<Location> result = new ArrayList<>();
        for (Location location : locations) {
            if (location.getCode() != code) {
                result.add(location);
            }
        }
        return result;
    }

    public static List<Location> read() {
        List<Location> list = new ArrayList<>();

        try {
            FileReader fileReader = new FileReader(
                    new File(Location.class.getSimpleName().toLowerCase() + ".txt")
            );
            Scanner scanner = new Scanner(fileReader);

            while (scanner.hasNextLine()) {
                list.add(Location.stringToObject(scanner.nextLine()));
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
}