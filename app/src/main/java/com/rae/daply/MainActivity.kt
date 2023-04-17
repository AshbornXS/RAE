package com.rae.daply

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.rae.daply.data.DataClass
import com.rae.daply.data.MyAdaptor
import com.rae.daply.data.UploadActivity
import com.rae.daply.login.LoginActivity


open class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fab: View = findViewById(R.id.fab)
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)

        val sair: Button = findViewById(R.id.buttonLogout)

        sair.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        val save = FirebaseAuth.getInstance().currentUser?.email?.replace("@etec.sp.gov.br", "")
            ?.replace(".", "-")

        val dbReference = FirebaseDatabase.getInstance()
        dbReference.reference.child("Users").child(save.toString()).child("userType").get()
            .addOnSuccessListener {
                val userType = it.value.toString()
                if (userType == "aluno") {
                    fab.visibility = View.GONE
                }
            }


        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setView(R.layout.loading_layout)
        val dialog: AlertDialog = builder.create()
        dialog.show()

        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.stackFromEnd = true
        linearLayoutManager.reverseLayout = true
        recyclerView.layoutManager = linearLayoutManager

        val avisosArrayList: List<DataClass> = ArrayList()

        val adapter = MyAdaptor(
            this,
            avisosArrayList as ArrayList<DataClass>
        )
        recyclerView.adapter = adapter

        val databaseReference: DatabaseReference =
            FirebaseDatabase.getInstance().getReference("RAE")
        dialog.show()

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                avisosArrayList.clear()
                for (itemSnapshot in snapshot.children) {
                    val dataClass = itemSnapshot.getValue(DataClass::class.java)
                    dataClass!!.key = itemSnapshot.key
                    avisosArrayList.add(dataClass)
                }
                adapter.notifyDataSetChanged()
                dialog.dismiss()
            }
            override fun onCancelled(error: DatabaseError) {
                dialog.dismiss()
            }
        })

        fab.setOnClickListener {
            val intent = Intent(this, UploadActivity::class.java)
            startActivity(intent)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }
}