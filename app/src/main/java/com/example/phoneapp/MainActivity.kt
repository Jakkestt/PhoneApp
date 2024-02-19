package com.example.phoneapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.lights.Light
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import coil.compose.AsyncImage
import com.example.phoneapp.ui.theme.PhoneAppTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        ).allowMainThreadQueries().build()
        val basicNotification = BasicNotification(this)

        setContent {
            App(
                database = db,
                basicNotification = basicNotification
            )
        }
    }
}

@Entity
data class User(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "profile_name", defaultValue = "Test Name") val profileName: String,
    @ColumnInfo(name = "selected_image") val selectedImage: String,
)

@Dao
interface UserDao {
    @Query("SELECT * FROM user")
    fun getAll(): List<User>

    @Insert
    fun insertUser(user: User)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateUser(user: User)

    @Delete
    fun deleteUser(user: User)
}

@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun App(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = "Chat",
    database: AppDatabase,
    basicNotification: BasicNotification
) {
    val context = LocalContext.current
    val postNotificationPermission =
        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
    LaunchedEffect(key1 = true) {
        if (!postNotificationPermission.status.isGranted) {
            postNotificationPermission.launchPermissionRequest()
        }
    }
    val sensorData = SensorDataDisplay(basicNotification = basicNotification)
    val userDao = database.userDao()
    val size: List<User> = userDao.getAll()
    if (size.isEmpty()) {
        val user = User(
            uid = 1,
            profileName = "Test Name",
            selectedImage = Uri.EMPTY.toString()
        )
        userDao.insertUser(user)
    }
    val users: List<User> = userDao.getAll()
    var selectedImage by remember {
        mutableStateOf<Uri>(Uri.parse(users[0].selectedImage))
    }

    var profileName by remember {
        mutableStateOf(users[0].profileName)
    }

    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val newUri = saveImageToInternalStorage(context, uri)
                selectedImage = newUri
                val user = User(
                    uid = users[0].uid,
                    profileName,
                    selectedImage.toString()
                )
                userDao.updateUser(user)
            }
        },
    )

    fun changeName(it: String) {
        profileName = it
        val user = User(
            uid = users[0].uid,
            profileName,
            selectedImage.toString()
        )
        userDao.updateUser(user)
    }

    fun launchPhotoPicker() {
        pickMedia.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination,
    ) {
        composable("Chat") {
            PhoneAppTheme {
                MainScreen(
                    onNavigateToProfile = {
                        navController.navigate("Profile")
                    },
                    selectedImage = selectedImage,
                    profileName = profileName
                )
            }
        }
        composable("Profile") {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onPickImage = { launchPhotoPicker() },
                selectedImage = selectedImage,
                profileName = profileName,
                changeName = { changeName(it) },
                sensorData
            )
        }
    }
}

@Composable
fun MainScreen(
    onNavigateToProfile: () -> Unit,
    selectedImage: Uri,
    profileName: String,
) {
    Column {
        Button(onClick = onNavigateToProfile) {
            Text(text = "Profile")
        }
        Conversation(
            messages = SampleData.conversationSample,
            selectedImage = selectedImage,
            profileName = profileName
        )
    }
}

@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onPickImage: () -> Unit,
    selectedImage: Uri,
    profileName: String,
    changeName: (String) -> Unit,
    data: Float,
) {
    Column {
        Button(onClick = onNavigateBack) {
            Text(text = "Back")
        }
        Button(onClick = { onPickImage() }) {
            Text(text = "Choose Image")
        }
        if (selectedImage != Uri.EMPTY) {
            AsyncImage(
                model = selectedImage,
                contentDescription = "Image",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable._50px_hl_gonarch_model),
                contentDescription = "Image",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
        TextField(
            value = profileName,
            onValueChange = { changeName(it) },
            label = { Text("Profile Name") }
        )
        Text(text = "Sensor Data: $data")
    }
}

data class Message(val author: String, val body: String)

@Composable
fun Function(
    msg: Message,
    selectedImage: Uri,
    profileName: String
) {
    Row(modifier = Modifier.padding(8.dp)) {
        if (selectedImage != Uri.EMPTY) {
            AsyncImage(
                model = selectedImage,
                contentDescription = "This is an example image",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
        } else {
            Image(painter = painterResource(
                id = R.drawable._50px_hl_gonarch_model),
                contentDescription = "Image",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        var isExpanded by remember { mutableStateOf(false) }
        val surfaceColor by animateColorAsState(
            if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            label = "",
        )

        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
            Text(
                text = profileName,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleSmall,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 1.dp,
                color = surfaceColor,
                modifier = Modifier
                    .animateContentSize()
                    .padding(1.dp)
            ) {
                Text(
                    text = msg.body,
                    modifier = Modifier.padding(all = 4.dp),
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Preview(name = "Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun PreviewFunction() {
    PhoneAppTheme {
        Surface {
            Function(Message(
                "Jarkko",
                "Hello"
            ), profileName = "Jarkko", selectedImage = Uri.EMPTY)
        }
    }
}

@Composable
fun Conversation(
    messages: List<Message>,
    selectedImage: Uri,
    profileName: String
) {
    LazyColumn {
        items(messages) { message ->
            Function(
                message,
                selectedImage = selectedImage,
                profileName = profileName
            )
        }
    }
}

@Preview
@Composable
fun PreviewConversation() {
    PhoneAppTheme {
        Conversation(
            SampleData.conversationSample,
            profileName = "Jarkko",
            selectedImage = Uri.EMPTY
        )
    }
}

fun saveImageToInternalStorage(context: Context, uri: Uri): Uri {
    val inputStream = context.contentResolver.openInputStream(uri)
    val outputStream = context.openFileOutput("image.jpg", Context.MODE_PRIVATE)
    inputStream?.use { input ->
        outputStream.use { output ->
            input.copyTo(output)
        }
    }
    val file = File(context.filesDir, "image.jpg")
    return Uri.fromFile(file)
}

private fun createNotificationChannel(context: Context) {
    // Create the NotificationChannel, but only on API 26+ because
    // the NotificationChannel class is not in the Support Library.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Hello"
        val descriptionText = "Hello"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("1", name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system.
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

private fun sendNotification(context: Context) {
    // Create the notification
    val builder = NotificationCompat.Builder(context, "default_channel_id")
        .setSmallIcon(R.drawable._50px_hl_gonarch_model)
        .setContentTitle("My Notification")
        .setContentText("This is a notification triggered from Jetpack Compose")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    with(NotificationManagerCompat.from(context)) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            // ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            // public fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
            //                                        grantResults: IntArray)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            return@with
        }
        // notificationId is a unique int for each notification that you must define.
        notify(Random.nextInt(), builder)
    }
}