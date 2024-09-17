package com.example.roomdatabase

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var userDao: UserDao

    private lateinit var edtUserName: EditText
    private lateinit var edtUserEmail: EditText
    private lateinit var btnAddUser: Button
    private lateinit var recVData: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private lateinit var btnUpdate: Button
    private lateinit var btnDelete: Button
    private lateinit var btnSelect: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize the database
        database = AppDatabase.createDatabase(this)
        userDao = database.userDao()

        edtUserName = findViewById(R.id.edtUserName)
        edtUserEmail = findViewById(R.id.edtUserEmail)
        btnAddUser = findViewById(R.id.btnAddUser)
        recVData = findViewById(R.id.recVData)
        btnUpdate = findViewById(R.id.btnUpdate)
        btnDelete = findViewById(R.id.btnDelete)
        btnSelect = findViewById(R.id.btnSelect)

        recVData.layoutManager = LinearLayoutManager(this)
        userAdapter = UserAdapter(emptyList())
        recVData.adapter = userAdapter

        loadUsers()



        btnAddUser.setOnClickListener {
            val name = edtUserName.text.toString()
            val email = edtUserEmail.text.toString()

            if (name.isNotEmpty() && email.isNotEmpty()) {
                // Insert the user into the database
                lifecycleScope.launch {
                    val user = UserEntity(name = name, email = email)
                    userDao.insertUser(user)
                    loadUsers()  // Reload users after inserting
                }
            } else {
                // Handle empty fields if necessary
                println("Please fill in both fields")
            }
        }

        btnUpdate.setOnClickListener {
            showUpdateDeleteDialog(isUpdate = true)
        }

        btnDelete.setOnClickListener {
            showUpdateDeleteDialog(isUpdate = false)
        }

        btnSelect.setOnClickListener {
            showSelectUserDialog()
        }
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            val users = userDao.getAllUsers()
            userAdapter.setUsers(users)
            edtUserName.text.clear()
            edtUserEmail.text.clear()
        }
    }

    private fun showUpdateDeleteDialog(isUpdate: Boolean) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_update_delete, null)
        val spinnerCriteria: Spinner = dialogView.findViewById(R.id.spinnerCriteria)
        val edtCriteriaValue: EditText = dialogView.findViewById(R.id.edtCriteriaValue)
        val edtNewValue: EditText = dialogView.findViewById(R.id.edtNewValue)

        val spinnerUpdateField: Spinner = dialogView.findViewById(R.id.spinnerUpdateField)

        // Set the visibility of edtNewValue based on the isUpdate flag
        edtNewValue.visibility = if (isUpdate) View.VISIBLE else View.GONE

        // Set spinnerUpdateField visibility based on the isUpdate flag
        spinnerUpdateField.visibility = if (isUpdate) View.VISIBLE else View.GONE

        val dialogTitle = if (isUpdate) "Update User" else "Delete User"
        
        val dialog = AlertDialog.Builder(this)
            .setTitle(dialogTitle)
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val criteria = spinnerCriteria.selectedItem.toString()
                val value = edtCriteriaValue.text.toString()
                val newValue = edtNewValue.text.toString()
                val updateField = spinnerUpdateField.selectedItem.toString()
                if (isUpdate) {
                    if (newValue.isNotEmpty()) {
                        updateUser(criteria, value, updateField, newValue)
                    } else {
                        println("Please fill in all fields for update")
                    }
                } else {
                    deleteUser(criteria, value)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        spinnerCriteria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCriteria = parent.getItemAtPosition(position).toString()
                if (!isUpdate && selectedCriteria == "ID") {
                    spinnerUpdateField.visibility = View.GONE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Handle case when nothing is selected if necessary
            }
        }

        dialog.show()
    }

    private fun showSelectUserDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_select_user, null)
        val spinnerCriteria: Spinner = dialogView.findViewById(R.id.spinnerCriteria)
        val edtCriteriaValue: EditText = dialogView.findViewById(R.id.edtCriteriaValue)
        val btnFetch: Button = dialogView.findViewById(R.id.btnFetch)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Select User")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        btnFetch.setOnClickListener {
            val criteria = spinnerCriteria.selectedItem.toString()
            val value = edtCriteriaValue.text.toString()

            if (value.isNotEmpty()) {
                lifecycleScope.launch {
                    val user = when (criteria) {
                        "Name" -> userDao.getUserByName(value)
                        "Email" -> userDao.getUserByEmail(value)
                        "ID" -> userDao.getUserById(value.toLong())
                        else -> null
                    }

                    user?.let {
                        showUserDetails(it)
                    } ?: run {
                        // Handle case where user is not found
                        println("No user found with the provided criteria")
                    }
                }
            } else {
                println("Please provide a value for criteria")
            }
        }

        dialog.show()
    }

    private fun updateUser(criteria: String, criteriaValue: String, updateField: String, newValue: String) {
        lifecycleScope.launch {
            when (criteria) {
                "Name" -> {
                    val user = userDao.getUserByName(criteriaValue)
                    user?.let {
                        val updatedUser = when (updateField) {
                            "Name" -> it.copy(name = newValue)
                            "Email" -> it.copy(email = newValue)
                            else -> it
                        }
                        userDao.updateUser(updatedUser)
                    }
                }

                "Email" -> {
                    val user = userDao.getUserByEmail(criteriaValue)
                    user?.let {
                        val updatedUser = when (updateField) {
                            "Name" -> it.copy(name = newValue)
                            "Email" -> it.copy(email = newValue)
                            else -> it
                        }
                        userDao.updateUser(updatedUser)
                    }
                }

                "ID" -> {
                    val user = userDao.getUserById(criteriaValue.toLong())
                    user?.let {
                        val updatedUser = when (updateField) {
                            "Name" -> it.copy(name = newValue)
                            "Email" -> it.copy(email = newValue)
                            else -> it
                        }
                        userDao.updateUser(updatedUser)
                    }
                }
            }
            loadUsers()
        }
    }

    private fun deleteUser(criteria: String, value: String) {
        // Implement the logic to delete user based on the selected criteria
        lifecycleScope.launch {
            when (criteria) {
                "Name" -> {
                    // Delete user by name
                    val user = userDao.getUserByName(value)
                    user?.let {
                        userDao.deleteUser(it)
                    }
                }
                "Email" -> {
                    // Delete user by email
                    val user = userDao.getUserByEmail(value)
                    user?.let {
                        userDao.deleteUser(it)
                    }
                }
                "ID" -> {
                    // Delete user by ID
                    val user = userDao.getUserById(value.toInt().toLong())
                    user?.let {
                        userDao.deleteUser(it)
                    }
                }
            }
            loadUsers()
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun showUserDetails(user: UserEntity) {
        val userDetailsView = layoutInflater.inflate(R.layout.dialog_user_details, null)
        val txtId: TextView = userDetailsView.findViewById(R.id.txtId)
        val txtName: TextView = userDetailsView.findViewById(R.id.txtName)
        val txtEmail: TextView = userDetailsView.findViewById(R.id.txtEmail)

        txtId.text = "ID: ${user.id}"
        txtName.text = "Name: ${user.name}"
        txtEmail.text = "Email: ${user.email}"

        AlertDialog.Builder(this)
            .setTitle("User Details")
            .setView(userDetailsView)
            .setPositiveButton("OK", null)
            .create()
            .show()
    }
}