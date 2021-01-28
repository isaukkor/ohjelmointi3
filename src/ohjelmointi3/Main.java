package ohjelmointi3;

import java.io.PrintWriter;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class Main {
	
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");

	public static void main(String[] args) throws FileNotFoundException {

		Character y = 'y';
		File inputFile = new File("input.txt");
		
		PrintWriter printWriter = null;
		try {printWriter = new PrintWriter(inputFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		Scanner scanner = new Scanner(System.in);
		//PrintWriter printWriter = new PrintWriter("asd.txt");
		boolean done = false;
		
		System.out.println("give name");
		String username = scanner.nextLine();

		while (!done) {
		
			System.out.println("type something");
			String userinput = scanner.nextLine();
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			String time = (sdf.format(timestamp));
			
			String stringToFile = "(" + time + ") " + "<" + username + "> " + userinput;
			
			printWriter.println (stringToFile);
			
		    System.out.println("print previous input? (y/n)");
			Character printInput = scanner.next().charAt(0);
			scanner.nextLine();
			if (printInput == y ) {
				System.out.println(stringToFile);
			}
		    System.out.println("done? (y/n)");
			printInput = scanner.next().charAt(0);
			scanner.nextLine();
			if (printInput == y ) {
				done = true;
			}
		}
	    printWriter.close ();
	    scanner.close();
	}
}
