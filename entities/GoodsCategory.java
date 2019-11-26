package entities;

import util.Util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GoodsCategory extends Common {
    public GoodsCategory() {}

    public GoodsCategory(int code, String name, String description){
        super(code, name, description);
    }

    public static GoodsCategory stringToObject(String text) {
        String[] attr = text.split(Util.SEPARATOR_REGEX);
        GoodsCategory element = new GoodsCategory();
        element.setCode(Integer.parseInt(attr[0]));
        element.setName(attr[1]);
        element.setDescription(attr[2]);
        return element;
    }

    public static GoodsCategory get(String filter) {
        List<GoodsCategory> elements = GoodsCategory.read();
        GoodsCategory result = null;
        for (GoodsCategory element : elements) {
            if (element.getName().equals(filter)) {
                result = element;
            }
        }
        return result;
    }

    public static GoodsCategory getByCode(int code) {
        List<GoodsCategory> elements = GoodsCategory.read();
        GoodsCategory result = null;
        for (GoodsCategory element : elements) {
            if (element.getCode() == code) {
                result = element;
            }
        }
        return result;
    }

    public static List<GoodsCategory> filterByNull() {
        List<GoodsCategory> goodsCategories = GoodsCategory.read();
        List<GoodsCategory> result = new ArrayList<>();
        for (GoodsCategory goodsCategory : goodsCategories) {
            if (Good.filterByCategory(goodsCategory.getCode()).isEmpty()) {
                result.add(goodsCategory);
            }
        }

        return result;
    }

    public static void delete(int code) {
        try {
            File tempDB = new File(GoodsCategory.class.getSimpleName().toLowerCase() + "_temp.txt");
            File db = new File(GoodsCategory.class.getSimpleName().toLowerCase() + ".txt");


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

    public static List<GoodsCategory> read() {
        List<GoodsCategory> list = new ArrayList<>();

        try {
            FileReader fileReader = new FileReader(
                    new File(GoodsCategory.class.getSimpleName().toLowerCase() + ".txt")
            );
            Scanner scanner = new Scanner(fileReader);

            while (scanner.hasNextLine()) {
                list.add(GoodsCategory.stringToObject(scanner.nextLine()));
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
