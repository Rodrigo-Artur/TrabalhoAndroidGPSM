import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contactos")
data class Contacto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nome: String,
    val telefone: String,
    val email: String,
    val endereco: String,
    val latitude: Double?,  // Para capturar a localização associada ao contacto
    val longitude: Double?
)