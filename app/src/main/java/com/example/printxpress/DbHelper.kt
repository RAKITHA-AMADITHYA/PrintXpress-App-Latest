package com.example.printxpress

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------- Data models ----------
data class User(val id: Long, val name: String, val email: String, val phone: String)
data class Product(val id: Long, val name: String, val category: String, val price: Double, val description: String, val color: String)
data class Order(
    val id: Long, val productId: Long, val productName: String, val material: String, val size: String,
    val quantity: Int, val customText: String, val fileName: String, val fileUri: String,
    val deliveryMethod: String, val address: String, val scheduleDate: String,
    val total: Double, val status: String, val createdAt: String
)
data class Promo(val id: Long, val title: String, val description: String, val code: String, val discount: Int, val minQty: Int)
data class Design(val id: Long, val title: String, val fileName: String, val fileUri: String, val customText: String, val createdAt: String)
data class Address(val id: Long, val label: String, val address: String)
data class SupportQuery(val id: Long, val subject: String, val message: String, val status: String, val createdAt: String)

/**
 * SQLite database (3NF):
 * users, categories, products, product_options, addresses, designs, promotions, orders, support_queries
 */
class DbHelper(context: Context) : SQLiteOpenHelper(context, "printxpress.db", null, 1) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE users(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            email TEXT NOT NULL UNIQUE,
            phone TEXT NOT NULL UNIQUE,
            password_hash TEXT NOT NULL)""")
        db.execSQL("""CREATE TABLE categories(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL UNIQUE)""")
        db.execSQL("""CREATE TABLE products(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            category_id INTEGER NOT NULL REFERENCES categories(id),
            name TEXT NOT NULL,
            unit_price REAL NOT NULL CHECK(unit_price > 0),
            description TEXT,
            color TEXT)""")
        db.execSQL("""CREATE TABLE product_options(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            product_id INTEGER NOT NULL REFERENCES products(id) ON DELETE CASCADE,
            option_type TEXT NOT NULL CHECK(option_type IN ('MATERIAL','SIZE')),
            value TEXT NOT NULL)""")
        db.execSQL("""CREATE TABLE addresses(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            label TEXT NOT NULL,
            address TEXT NOT NULL)""")
        db.execSQL("""CREATE TABLE designs(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            title TEXT NOT NULL,
            file_name TEXT,
            file_uri TEXT,
            custom_text TEXT,
            created_at TEXT NOT NULL)""")
        db.execSQL("""CREATE TABLE promotions(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            description TEXT,
            code TEXT NOT NULL UNIQUE,
            discount INTEGER NOT NULL CHECK(discount BETWEEN 1 AND 90),
            min_qty INTEGER NOT NULL DEFAULT 1)""")
        db.execSQL("""CREATE TABLE orders(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL REFERENCES users(id),
            product_id INTEGER NOT NULL REFERENCES products(id),
            promo_id INTEGER REFERENCES promotions(id),
            material TEXT NOT NULL,
            size TEXT NOT NULL,
            quantity INTEGER NOT NULL CHECK(quantity > 0),
            custom_text TEXT,
            file_name TEXT,
            file_uri TEXT,
            delivery_method TEXT NOT NULL,
            address TEXT,
            schedule_date TEXT NOT NULL,
            total REAL NOT NULL,
            status TEXT NOT NULL,
            created_at TEXT NOT NULL)""")
        db.execSQL("""CREATE TABLE support_queries(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            subject TEXT NOT NULL,
            message TEXT NOT NULL,
            status TEXT NOT NULL,
            created_at TEXT NOT NULL)""")
        seed(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        listOf("support_queries", "orders", "promotions", "designs", "addresses",
            "product_options", "products", "categories", "users")
            .forEach { db.execSQL("DROP TABLE IF EXISTS $it") }
        onCreate(db)
    }

    // ---------- Seed data ----------
    private fun seed(db: SQLiteDatabase) {
        // category, name, unit price (LKR), description, colour, materials, sizes
        val products = listOf(
            arrayOf("Business Cards", "Standard Business Card", "15", "Full-colour, double-sided business cards.", "#1E3A8A", "Matte 300gsm;Glossy 350gsm;Textured Linen", "3.5 x 2 in;3.5 x 2 in (Rounded)"),
            arrayOf("Business Cards", "Premium Business Card", "35", "Luxury cards with soft-touch or foil finish.", "#B8860B", "Soft-touch 400gsm;Gold Foil;Silver Foil", "3.5 x 2 in;Square 2.5 x 2.5 in"),
            arrayOf("Flyers", "Flyer", "25", "Promote events, sales and menus.", "#DC2626", "Gloss 130gsm;Matte 170gsm", "A5;A4;DL"),
            arrayOf("Posters", "Poster", "450", "Vivid posters for shops and events.", "#7C3AED", "Satin 200gsm;Gloss 250gsm", "A3;A2;A1"),
            arrayOf("Banners", "Vinyl Banner", "2500", "Weatherproof banners for indoor and outdoor use.", "#059669", "Flex 440gsm;Mesh Vinyl", "3 x 2 ft;6 x 3 ft;10 x 4 ft"),
            arrayOf("Stickers", "Custom Stickers", "20", "Labels and stickers in any shape.", "#F59E0B", "Gloss Vinyl;Matte Paper;Transparent", "2 x 2 in;3 x 3 in;Die-cut"),
            arrayOf("T-Shirts", "Printed T-Shirt", "1800", "Custom printed tees for teams and events.", "#0EA5E9", "Cotton 180gsm;Dri-fit Polyester", "S;M;L;XL;XXL"),
            arrayOf("Mugs", "Custom Mug", "1200", "Photo and logo mugs – great gifts.", "#DB2777", "White Ceramic;Magic Colour-Change", "11 oz;15 oz")
        )
        for (p in products) {
            val catId = categoryId(db, p[0])
            val pid = db.insert("products", null, ContentValues().apply {
                put("category_id", catId); put("name", p[1]); put("unit_price", p[2].toDouble())
                put("description", p[3]); put("color", p[4])
            })
            p[5].split(";").forEach { addOption(db, pid, "MATERIAL", it) }
            p[6].split(";").forEach { addOption(db, pid, "SIZE", it) }
        }
        val promos = listOf(
            arrayOf("Welcome Offer", "5% off your first order", "WELCOME5", "5", "1"),
            arrayOf("Bulk Order Saver", "10% off when you order 100 or more items", "BULK10", "10", "100"),
            arrayOf("Avurudu Festive Designs", "15% off festive New Year prints", "AVURUDU15", "15", "1"),
            arrayOf("Vesak & Christmas Season", "12% off seasonal posters and stickers", "SEASON12", "12", "1")
        )
        for (p in promos) {
            db.insert("promotions", null, ContentValues().apply {
                put("title", p[0]); put("description", p[1]); put("code", p[2])
                put("discount", p[3].toInt()); put("min_qty", p[4].toInt())
            })
        }
    }

    private fun categoryId(db: SQLiteDatabase, name: String): Long {
        db.rawQuery("SELECT id FROM categories WHERE name=?", arrayOf(name)).use {
            if (it.moveToFirst()) return it.getLong(0)
        }
        return db.insert("categories", null, ContentValues().apply { put("name", name) })
    }

    private fun addOption(db: SQLiteDatabase, productId: Long, type: String, value: String) {
        db.insert("product_options", null, ContentValues().apply {
            put("product_id", productId); put("option_type", type); put("value", value)
        })
    }

    // ---------- Helpers ----------
    private fun now(): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
    private fun Cursor.str(i: Int): String = if (isNull(i)) "" else getString(i)
    private fun <T> query(sql: String, args: Array<String> = emptyArray(), map: (Cursor) -> T): List<T> =
        readableDatabase.rawQuery(sql, args).use { c ->
            val out = mutableListOf<T>()
            while (c.moveToNext()) out.add(map(c))
            out
        }

    // ---------- Users ----------
    fun registerUser(name: String, email: String, phone: String, passwordHash: String): Long =
        writableDatabase.insert("users", null, ContentValues().apply {
            put("name", name); put("email", email); put("phone", phone); put("password_hash", passwordHash)
        })

    /** column must be "email" or "phone" */
    fun isTaken(column: String, value: String, excludeUserId: Long = -1): Boolean {
        require(column == "email" || column == "phone") { "unsupported column: $column" }
        return query("SELECT id FROM users WHERE $column=? AND id<>?",
            arrayOf(value, excludeUserId.toString())) { it.getLong(0) }.isNotEmpty()
    }

    fun login(identifier: String, passwordHash: String): Long =
        query("SELECT id FROM users WHERE (email=? OR phone=?) AND password_hash=?",
            arrayOf(identifier, identifier, passwordHash)) { it.getLong(0) }.firstOrNull() ?: -1L

    fun getUser(id: Long): User? =
        query("SELECT id,name,email,phone FROM users WHERE id=?", arrayOf(id.toString())) {
            User(it.getLong(0), it.str(1), it.str(2), it.str(3))
        }.firstOrNull()

    /** Returns false instead of crashing if the email/phone was taken between check and save. */
    fun updateUser(id: Long, name: String, email: String, phone: String): Boolean = try {
        writableDatabase.update("users", ContentValues().apply {
            put("name", name); put("email", email); put("phone", phone)
        }, "id=?", arrayOf(id.toString())) > 0
    } catch (e: SQLiteConstraintException) {
        false
    }

    // ---------- Products ----------
    fun getCategories(): List<String> = query("SELECT name FROM categories ORDER BY name") { it.str(0) }

    fun getProducts(category: String? = null, search: String = ""): List<Product> {
        val sql = StringBuilder(
            "SELECT p.id,p.name,c.name,p.unit_price,p.description,p.color FROM products p " +
            "JOIN categories c ON c.id=p.category_id WHERE 1=1")
        val args = mutableListOf<String>()
        if (category != null) { sql.append(" AND c.name=?"); args.add(category) }
        if (search.isNotBlank()) {
            // Escape LIKE wildcards so typing "%" or "_" searches for those characters.
            val term = "%" + search.trim()
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
            sql.append(" AND (p.name LIKE ? ESCAPE '\\' OR c.name LIKE ? ESCAPE '\\'" +
                " OR p.id IN (SELECT product_id FROM product_options WHERE value LIKE ? ESCAPE '\\'))")
            repeat(3) { args.add(term) }
        }
        sql.append(" ORDER BY c.name, p.name")
        return query(sql.toString(), args.toTypedArray()) {
            Product(it.getLong(0), it.str(1), it.str(2), it.getDouble(3), it.str(4), it.str(5))
        }
    }

    fun getProduct(id: Long): Product? =
        query("SELECT p.id,p.name,c.name,p.unit_price,p.description,p.color FROM products p " +
              "JOIN categories c ON c.id=p.category_id WHERE p.id=?", arrayOf(id.toString())) {
            Product(it.getLong(0), it.str(1), it.str(2), it.getDouble(3), it.str(4), it.str(5))
        }.firstOrNull()

    fun getOptions(productId: Long, type: String): List<String> =
        query("SELECT value FROM product_options WHERE product_id=? AND option_type=? ORDER BY id",
            arrayOf(productId.toString(), type)) { it.str(0) }

    // ---------- Orders ----------
    fun placeOrder(
        userId: Long, productId: Long, promoId: Long?, material: String, size: String, qty: Int,
        customText: String, fileName: String, fileUri: String, method: String, address: String,
        date: String, total: Double
    ): Long = writableDatabase.insert("orders", null, ContentValues().apply {
        put("user_id", userId); put("product_id", productId)
        if (promoId == null) putNull("promo_id") else put("promo_id", promoId)
        put("material", material); put("size", size); put("quantity", qty)
        put("custom_text", customText); put("file_name", fileName); put("file_uri", fileUri)
        put("delivery_method", method); put("address", address); put("schedule_date", date)
        put("total", total); put("status", OrderStatus.PENDING); put("created_at", now())
    })

    private val orderSelect =
        "SELECT o.id,o.product_id,p.name,o.material,o.size,o.quantity,o.custom_text,o.file_name,o.file_uri," +
        "o.delivery_method,o.address,o.schedule_date,o.total,o.status,o.created_at " +
        "FROM orders o JOIN products p ON p.id=o.product_id"

    private fun toOrder(c: Cursor) = Order(
        c.getLong(0), c.getLong(1), c.str(2), c.str(3), c.str(4), c.getInt(5), c.str(6), c.str(7), c.str(8),
        c.str(9), c.str(10), c.str(11), c.getDouble(12), c.str(13), c.str(14)
    )

    fun getOrders(userId: Long): List<Order> =
        query("$orderSelect WHERE o.user_id=? ORDER BY o.id DESC", arrayOf(userId.toString())) { toOrder(it) }

    /** Scoped to the owner so a stale link can never open someone else's order. */
    fun getOrder(id: Long, userId: Long): Order? =
        query("$orderSelect WHERE o.id=? AND o.user_id=?", arrayOf(id.toString(), userId.toString())) { toOrder(it) }
            .firstOrNull()

    fun updateOrderStatus(id: Long, userId: Long, status: String) =
        writableDatabase.update("orders", ContentValues().apply { put("status", status) },
            "id=? AND user_id=?", arrayOf(id.toString(), userId.toString())) > 0

    fun rescheduleOrder(id: Long, userId: Long, date: String) =
        writableDatabase.update("orders", ContentValues().apply { put("schedule_date", date) },
            "id=? AND user_id=?", arrayOf(id.toString(), userId.toString())) > 0

    // ---------- Addresses ----------
    fun addAddress(userId: Long, label: String, address: String): Long =
        writableDatabase.insert("addresses", null, ContentValues().apply {
            put("user_id", userId); put("label", label); put("address", address)
        })

    fun getAddresses(userId: Long): List<Address> =
        query("SELECT id,label,address FROM addresses WHERE user_id=? ORDER BY id", arrayOf(userId.toString())) {
            Address(it.getLong(0), it.str(1), it.str(2))
        }

    fun deleteAddress(id: Long, userId: Long) =
        writableDatabase.delete("addresses", "id=? AND user_id=?", arrayOf(id.toString(), userId.toString())) > 0

    // ---------- Designs ----------
    fun addDesign(userId: Long, title: String, fileName: String, fileUri: String, customText: String): Long =
        writableDatabase.insert("designs", null, ContentValues().apply {
            put("user_id", userId); put("title", title); put("file_name", fileName)
            put("file_uri", fileUri); put("custom_text", customText); put("created_at", now())
        })

    fun getDesigns(userId: Long): List<Design> =
        query("SELECT id,title,file_name,file_uri,custom_text,created_at FROM designs WHERE user_id=? ORDER BY id DESC",
            arrayOf(userId.toString())) {
            Design(it.getLong(0), it.str(1), it.str(2), it.str(3), it.str(4), it.str(5))
        }

    fun deleteDesign(id: Long, userId: Long) =
        writableDatabase.delete("designs", "id=? AND user_id=?", arrayOf(id.toString(), userId.toString())) > 0

    // ---------- Promotions ----------
    fun getPromos(): List<Promo> =
        query("SELECT id,title,description,code,discount,min_qty FROM promotions ORDER BY discount DESC") {
            Promo(it.getLong(0), it.str(1), it.str(2), it.str(3), it.getInt(4), it.getInt(5))
        }

    fun findPromo(code: String): Promo? =
        query("SELECT id,title,description,code,discount,min_qty FROM promotions WHERE UPPER(code)=UPPER(?)", arrayOf(code.trim())) {
            Promo(it.getLong(0), it.str(1), it.str(2), it.str(3), it.getInt(4), it.getInt(5))
        }.firstOrNull()

    // ---------- Support queries ----------
    fun addQuery(userId: Long, subject: String, message: String): Long =
        writableDatabase.insert("support_queries", null, ContentValues().apply {
            put("user_id", userId); put("subject", subject); put("message", message)
            put("status", "Open"); put("created_at", now())
        })

    fun getQueries(userId: Long): List<SupportQuery> =
        query("SELECT id,subject,message,status,created_at FROM support_queries WHERE user_id=? ORDER BY id DESC",
            arrayOf(userId.toString())) {
            SupportQuery(it.getLong(0), it.str(1), it.str(2), it.str(3), it.str(4))
        }
}
