package edu.umkc.cs5573.isa;

import java.io.File;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

public class SQLiteConnectionManager {
	void sqlTest() {
		SQLiteConnection db = new SQLiteConnection(new File("res/db/database.db"));
		try {
			db.open(true);
			SQLiteStatement st;
			st = db.prepare("SELECT order_id FROM orders WHERE quantity >= ?");
			//st.bind(1, minimumQuantity);
			while (st.step()) {
			//  orders.add(st.columnLong(0));
			}
			st.dispose();
			db.dispose();
		} catch (SQLiteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
