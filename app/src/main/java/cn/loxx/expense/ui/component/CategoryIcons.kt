package cn.loxx.expense.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Train
import androidx.compose.ui.graphics.vector.ImageVector

/** Maps stored icon names to Material icon vectors. */
object CategoryIcons {
    val selectable: List<Pair<String, ImageVector>> = listOf(
        "directions_car" to Icons.Filled.DirectionsCar,
        "local_taxi" to Icons.Filled.LocalTaxi,
        "train" to Icons.Filled.Train,
        "flight" to Icons.Filled.Flight,
        "hotel" to Icons.Filled.Hotel,
        "restaurant" to Icons.Filled.Restaurant,
        "phone" to Icons.Filled.Phone,
        "inventory_2" to Icons.Filled.Inventory2,
        "shopping_cart" to Icons.Filled.ShoppingCart,
        "card_giftcard" to Icons.Filled.CardGiftcard,
        "receipt" to Icons.Filled.Receipt,
        "more_horiz" to Icons.Filled.MoreHoriz,
    )

    private val byName = selectable.associate { (name, icon) -> name to icon }

    fun fromName(name: String): ImageVector = byName[name] ?: Icons.Filled.MoreHoriz
}
