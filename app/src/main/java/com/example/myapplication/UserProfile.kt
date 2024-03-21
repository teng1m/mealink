package com.example.myapplication
import android.content.ContentValues.TAG
import androidx.compose.runtime.Composable
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.ContentScale
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import coil.compose.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage

private lateinit var auth: FirebaseAuth
private lateinit var storage: FirebaseStorage

@Composable
fun UserProfileScreen() {
    auth = Firebase.auth
    storage = Firebase.storage
    var storageRef = storage.reference
    val user = auth.currentUser

    var isLoading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var userInfo by remember { mutableStateOf<Map<String, Any>?>(null) }
    var imageURI by remember { mutableStateOf("") }

    val docRef = user?.let { db.collection("users").document(it.uid) }
    docRef?.get()?.addOnSuccessListener { document ->
        if (document != null) {
            Log.d(TAG, "DocumentSnapshot data: ${document.data}")
            userInfo = document.data
            auth.currentUser?.let {
                storageRef.child(it.uid).downloadUrl.addOnSuccessListener {it ->
                    if (it != null) {
                        imageURI = it.toString()
                        isLoading = false
                    }
                }.addOnFailureListener {
                    isLoading = false
                }
            }
        } else {
            Log.d(TAG, "No such document")
            failed = true
        }
    }?.addOnFailureListener { exception ->
        Log.d(TAG, "get failed with ", exception)
        failed = true
    }


    if (isLoading) {
        LoadingScreen()
    } else if (!failed) {
        ProfileScreen(
            userInfo?.get("name").toString(),
            auth.currentUser?.email.toString(),
            userInfo?.get("location").toString(),
            userInfo?.get("type").toString(),
            imageURI
        )
    } else {
        LoadFailScreen()
    }
}

@Composable
fun ProfileScreen(userName: String, userEmail: String, userLocation: String, type: String, imageURI: String) {
    var name by remember { mutableStateOf(userName) }
    var email by remember { mutableStateOf(userEmail) }
    var location by remember {mutableStateOf(userLocation)}
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row{
            Spacer(Modifier.weight(1f))
            signout(name)
        }
        accountHeader(type, name)
        userPfp(imageURI)
        nameField(name = name, onNameChange = {name = it})
        emailField(email = email, onEmailChange = {email = it})
        locationField(location = location, onLocationChange = {location = it})
        saveButton(name, location)
        if (type == "foodDonor") {
            createOfferButton(name)
        } else {
            searchOffersButton(name)
        }
    }
}
@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(100.dp)
                .padding(16.dp)
                .align(Alignment.Center),
            color = Color(0xFF00BF81)
        )
    }
}

@Composable
fun LoadFailScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text(
            text = "Failed to load data. Please try again later.",
            modifier = Modifier
                .align(Alignment.Center),
        )
    }
}

@Composable
fun accountHeader(type: String, name: String) {
    var subHeader = if (type == "foodDonor") "Food Donor" else "Food Receiver"
    Text(text = "Welcome $name!",
        style=MaterialTheme.typography.h4,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp)
    )
    Text(text = "Manage Your $subHeader Account",
        style=MaterialTheme.typography.h6,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun userPfp(imageURI: String) {
    var pfp: String? by remember { mutableStateOf(imageURI) }
    val painter = rememberAsyncImagePainter(
        if (pfp.isNullOrEmpty()) {
            R.drawable.undraw_breakfast_psiw
            // default pfp
        } else {
            pfp
        }
    )

    var storageRef = storage.reference
    var photoUri: Uri? by remember { mutableStateOf(null) }
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        photoUri = uri
        val inputStream = context.contentResolver.openInputStream(photoUri!!)
        val fileName = "${auth.currentUser?.uid}"
        val imageRef = storageRef.child(fileName)
        var uploadTask = imageRef.putStream(inputStream!!)

        uploadTask.addOnFailureListener {
            println("upload failed")
        }.addOnSuccessListener { taskSnapshot ->
            println("upload success")
            pfp = photoUri.toString()
        }
    }

    Column (
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card (
            shape = CircleShape,
            modifier = Modifier.size(150.dp),

            ){
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .wrapContentSize()
                    .clickable {
                        launcher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                )
        }
        Text(text = "Tap picture to change profile picture", Modifier.padding(top = 8.dp))
    }
}
@Composable
fun nameField(name: String, onNameChange: (String) -> Unit) {
    OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
}

@Composable
fun emailField(email: String, onEmailChange: (String) -> Unit) {
    OutlinedTextField(value = email, onValueChange = onEmailChange, label = {Text("Email")}, modifier = Modifier.fillMaxWidth(), enabled = false)
}

@Composable
fun locationField(location: String, onLocationChange: (String) -> Unit) {
    OutlinedTextField(value = location, onValueChange = onLocationChange, label = {Text("Postal Code")}, modifier = Modifier.fillMaxWidth())
}
@Composable
fun saveButton(name: String, location: String) {
    ExtendedFloatingActionButton(
        onClick = {saveDetails(name, location)},
        text = {Text("Save")},
        backgroundColor = Color(0xFF00BF81),
        elevation = FloatingActionButtonDefaults.elevation(0.dp),
        contentColor = Color(0xFFFFFFFF),
    )
}

fun saveDetails(name: String, location: String) {
    auth.currentUser?.let { db.collection("users").document(it.uid)
        .update("name", name, "location", location)
    }
}

@Composable
fun createOfferButton(name: String) {
    val context = LocalContext.current
    ExtendedFloatingActionButton(
        onClick = {createOfferRedirect(name, context)},
        text = {Text("Create Offer")},
        backgroundColor = Color(0xFF00BF81),
        elevation = FloatingActionButtonDefaults.elevation(0.dp),
        contentColor = Color(0xFFFFFFFF),
    )
}

fun createOfferRedirect(name: String, context: Context) {
    val intent = Intent(context, OfferCreation::class.java)
    context.startActivity(intent, null)
}

@Composable
fun searchOffersButton(name: String) {
    val context = LocalContext.current
    ExtendedFloatingActionButton(
        onClick = {searchOffersRedirect(name, context)},
        text = {Text("Search Offers")},
        backgroundColor = Color(0xFF00BF81),
        elevation = FloatingActionButtonDefaults.elevation(0.dp),
        contentColor = Color(0xFFFFFFFF),
    )
}

fun searchOffersRedirect(name: String, context: Context) {
    val intent = Intent(context, SearchOffers::class.java)
    context.startActivity(intent, null)
}

@Composable
fun signout(name: String) {
    val context = LocalContext.current
    ExtendedFloatingActionButton(
        onClick = {signoutAction(name, context)},
        text = {Text("Sign Out")},
        backgroundColor = Color(0xFF00BF81),
        elevation = FloatingActionButtonDefaults.elevation(0.dp),
        contentColor = Color(0xFFFFFFFF),
    )
}

fun signoutAction(name: String, context: Context) {
    val user = FirebaseAuth.getInstance()
    user.signOut()

    val userCheck = FirebaseAuth.getInstance().getCurrentUser();
    if (userCheck == null) {
        // User is signed out
        Log.d(TAG, "signOut:success")
        // navController.navigate(Screens.Profile.route)
        val intent = Intent(context, MainActivity::class.java)
        context.startActivity(intent, null)

        Toast.makeText(
            context,
            "Signed Out $name Successfully.",
            Toast.LENGTH_SHORT,
        ).show()

    } else {
        // User is not signed out
        Log.w(TAG, "signOut:failure")
        Toast.makeText(
            context,
            "Sign Out failed.",
            Toast.LENGTH_SHORT,
        ).show()
    }
}