package com.example.printxpress

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout

class PlaceOrderActivity : BaseActivity() {

    private data class Price(val subtotal: Double, val discount: Double, val delivery: Double, val promo: Promo?) {
        val total get() = subtotal - discount + delivery
    }

    private lateinit var product: Product
    private var fileUri = ""
    private var fileName = ""
    private var scheduleDate = ""
    private var addresses = listOf<Address>()

    private lateinit var spMaterial: Spinner
    private lateinit var spSize: Spinner
    private lateinit var spAddress: Spinner
    private lateinit var etQty: EditText
    private lateinit var etText: EditText
    private lateinit var etPromo: EditText
    private lateinit var etDate: EditText
    private lateinit var tilQty: TextInputLayout
    private lateinit var tilText: TextInputLayout
    private lateinit var tilPromo: TextInputLayout
    private lateinit var tilDate: TextInputLayout
    private lateinit var tvFile: TextView
    private lateinit var tvSummary: TextView
    private lateinit var rbDelivery: RadioButton
    private lateinit var cbSave: CheckBox

    private val pickFile = registerForActivityResult(OpenPersistableDocument()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        val (name, size) = queryFileInfo(this, uri)
        if (size > MAX_FILE_BYTES) { toast("File is too large. Maximum size is 25 MB."); return@registerForActivityResult }
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) { }
        fileUri = uri.toString()
        fileName = name
        tvFile.text = "📎 $name"
        tilText.error = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val p = db.getProduct(intent.getLongExtra("productId", -1))
        if (p == null) { toast("Product not found"); finish(); return }
        product = p
        setupScreen(R.layout.activity_place_order, "Order: ${product.name}")

        // Views
        spMaterial = findViewById(R.id.spMaterial); spSize = findViewById(R.id.spSize); spAddress = findViewById(R.id.spAddress)
        etQty = findViewById(R.id.etQty); etText = findViewById(R.id.etText)
        etPromo = findViewById(R.id.etPromo); etDate = findViewById(R.id.etDate)
        tilQty = findViewById(R.id.tilQty); tilText = findViewById(R.id.tilText)
        tilPromo = findViewById(R.id.tilPromo); tilDate = findViewById(R.id.tilDate)
        tvFile = findViewById(R.id.tvFile); tvSummary = findViewById(R.id.tvSummary)
        rbDelivery = findViewById(R.id.rbDelivery); cbSave = findViewById(R.id.cbSaveDesign)

        // Product header / sample preview
        findViewById<View>(R.id.vPreview).setBackgroundColor(Color.parseColor(product.color))
        findViewById<TextView>(R.id.tvPreviewName).text = product.name
        findViewById<TextView>(R.id.tvProductDesc).text = "${product.category} • ${product.description}"
        findViewById<TextView>(R.id.tvUnitPrice).text = "${lkr(product.price)} per unit"

        spMaterial.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, db.getOptions(product.id, "MATERIAL"))
        spSize.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, db.getOptions(product.id, "SIZE"))

        etQty.setText("1")
        etQty.doAfterTextChanged { tilQty.error = null; updateSummary() }
        etPromo.doAfterTextChanged { tilPromo.error = null; updateSummary() }
        etText.doAfterTextChanged { tilText.error = null }

        findViewById<Button>(R.id.btnUpload).setOnClickListener {
            pickFile.launch(arrayOf("image/*", "application/pdf"))
        }
        findViewById<Button>(R.id.btnUseSaved).setOnClickListener { chooseSavedDesign() }

        val deliveryBox = findViewById<View>(R.id.deliveryBox)
        val tvPickup = findViewById<View>(R.id.tvPickupInfo)
        findViewById<RadioGroup>(R.id.rgMethod).setOnCheckedChangeListener { _, checkedId ->
            val delivery = checkedId == R.id.rbDelivery
            deliveryBox.visibility = if (delivery) View.VISIBLE else View.GONE
            tvPickup.visibility = if (delivery) View.GONE else View.VISIBLE
            updateSummary()
        }
        findViewById<Button>(R.id.btnAddAddress).setOnClickListener {
            showAddAddressDialog(this, db, uid) {
                loadAddresses()
                if (addresses.isNotEmpty()) spAddress.setSelection(addresses.size - 1)
            }
        }

        etDate.setOnClickListener {
            pickDate(this) { d -> scheduleDate = d; etDate.setText(d); tilDate.error = null }
        }

        findViewById<Button>(R.id.btnPlaceOrder).setOnClickListener { validateAndConfirm() }

        loadAddresses()
        updateSummary()
    }

    private fun loadAddresses() {
        addresses = db.getAddresses(uid)
        val labels = if (addresses.isEmpty()) listOf("No saved address – tap + Add")
                     else addresses.map { "${it.label}: ${it.address}" }
        spAddress.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
    }

    private fun chooseSavedDesign() {
        val designs = db.getDesigns(uid)
        if (designs.isEmpty()) { toast("You have no saved designs yet"); return }
        AlertDialog.Builder(this)
            .setTitle("Choose a saved design")
            .setItems(designs.map { it.title }.toTypedArray()) { _, i ->
                val d = designs[i]
                fileUri = d.fileUri; fileName = d.fileName
                tvFile.text = if (d.fileName.isNotEmpty()) "⭐ ${d.title} (${d.fileName})" else "⭐ ${d.title}"
                if (d.customText.isNotEmpty()) etText.setText(d.customText)
                cbSave.isChecked = false
            }
            .show()
    }

    private fun calculate(): Price {
        val qty = etQty.text.toString().toIntOrNull() ?: 0
        val subtotal = qty * product.price
        val code = etPromo.text.toString().trim()
        val promo = if (code.isNotEmpty()) db.findPromo(code) else null
        val discount = if (promo != null && qty >= promo.minQty) subtotal * promo.discount / 100.0 else 0.0
        val delivery = if (rbDelivery.isChecked) DELIVERY_FEE else 0.0
        return Price(subtotal, discount, delivery, promo)
    }

    private fun updateSummary() {
        val qty = etQty.text.toString().toIntOrNull() ?: 0
        val p = calculate()
        val sb = StringBuilder()
        sb.append("Subtotal ($qty × ${lkr(product.price)}): ${lkr(p.subtotal)}\n")
        if (p.discount > 0) sb.append("Discount ${p.promo!!.code} (-${p.promo.discount}%): -${lkr(p.discount)}\n")
        else if (p.promo != null) sb.append("Code ${p.promo.code} needs ${p.promo.minQty}+ items\n")
        if (p.delivery > 0) sb.append("Delivery fee: ${lkr(p.delivery)}\n")
        sb.append("TOTAL: ${lkr(p.total)}")
        tvSummary.text = sb.toString()
    }

    private fun validateAndConfirm() {
        tilQty.error = null; tilText.error = null; tilDate.error = null; tilPromo.error = null
        var ok = true

        val qty = etQty.text.toString().toIntOrNull()
        if (qty == null || qty < 1) { tilQty.error = "Enter a quantity of at least 1"; ok = false }
        else if (qty > 10000) { tilQty.error = "Maximum 10,000 items per order"; ok = false }

        val text = etText.text.toString().trim()
        if (fileUri.isEmpty() && text.isEmpty()) { tilText.error = "Upload artwork or enter custom text"; ok = false }
        else if (text.length > 200) { tilText.error = "Maximum 200 characters"; ok = false }

        val delivery = rbDelivery.isChecked
        val addressIndex = spAddress.selectedItemPosition
        if (delivery && addresses.isEmpty()) { toast("Please add a delivery address"); ok = false }
        else if (delivery && addressIndex !in addresses.indices) { toast("Please choose a delivery address"); ok = false }

        if (scheduleDate.isEmpty()) { tilDate.error = "Select a pickup / delivery date"; ok = false }

        // Spinners are empty if a product has no options – never dereference blindly.
        val material = spMaterial.selectedItem?.toString().orEmpty()
        val size = spSize.selectedItem?.toString().orEmpty()
        if (material.isEmpty() || size.isEmpty()) {
            toast("This product has no material or size options yet"); ok = false
        }

        val code = etPromo.text.toString().trim()
        val price = calculate()
        if (code.isNotEmpty()) {
            if (price.promo == null) { tilPromo.error = "Invalid promo code"; ok = false }
            else if ((qty ?: 0) < price.promo.minQty) { tilPromo.error = "This code needs ${price.promo.minQty}+ items"; ok = false }
        }
        if (!ok) { toast("Please fix the highlighted fields"); return }

        val method = if (delivery) "Delivery" else "Pickup"
        val address = if (delivery) addresses[addressIndex].address else PICKUP_LOCATION

        AlertDialog.Builder(this)
            .setTitle("Confirm your order")
            .setMessage("${product.name}\n$qty × $material, $size\n$method on $scheduleDate\n$address\n\nTotal: ${lkr(price.total)}")
            .setPositiveButton("Place order") { _, _ ->
                saveOrder(qty!!, material, size, text, method, address, price)
            }
            .setNegativeButton("Edit", null)
            .show()
    }

    private fun saveOrder(qty: Int, material: String, size: String, text: String, method: String, address: String, price: Price) {
        val orderId = db.placeOrder(
            uid, product.id, price.promo?.id, material, size, qty, text,
            fileName, fileUri, method, address, scheduleDate, price.total
        )
        if (orderId == -1L) { toast("Could not save the order. Please try again."); return }

        if (cbSave.isChecked) db.addDesign(uid, "${product.name} design", fileName, fileUri, text)

        Notifier.show(this, Notifier.CH_ORDERS, orderId.toInt(),
            "✅ Order #$orderId confirmed",
            "${product.name} × $qty • ${lkr(price.total)} • $method on $scheduleDate", orderId)
        toast("Order placed!")
        startActivity(Intent(this, OrderDetailActivity::class.java).putExtra("orderId", orderId))
        finish()
    }
}
