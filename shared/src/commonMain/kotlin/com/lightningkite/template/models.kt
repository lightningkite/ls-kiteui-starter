package com.lightningkite.template

import com.lightningkite.EmailAddress
import com.lightningkite.GeoCoordinate
import com.lightningkite.UUID
import com.lightningkite.lightningdb.GenerateDataClassPaths
import com.lightningkite.lightningdb.HasId
import kotlinx.serialization.Serializable


@Serializable
@GenerateDataClassPaths
data class User(
    override val _id: UUID = UUID.random(),
    val email: EmailAddress,
    val name: String,
    val role: UserRole = UserRole.User,
) : HasId<UUID>

@Serializable
enum class UserRole {
    User,
    Admin,
    Developer,
    Root
}


@Serializable
enum class UsState(val text: String, val coordinate: GeoCoordinate) {
    AL("Alabama", GeoCoordinate(32.806671, -86.791130)),
    AK("Alaska", GeoCoordinate(61.370716, -152.404419)),
    AZ("Arizona", GeoCoordinate(33.729759, -111.431221)),
    AR("Arkansas", GeoCoordinate(34.969704, -92.373123)),
    CA("California", GeoCoordinate(36.116203, -119.681564)),
    CO("Colorado", GeoCoordinate(39.059811, -105.311104)),
    CT("Connecticut", GeoCoordinate(41.597782, -72.755371)),
    DE("Delaware", GeoCoordinate(39.318523, -75.507141)),
    DC("District Of Columbia", GeoCoordinate(38.897438, -77.026817)),
    FL("Florida", GeoCoordinate(27.766279, -81.686783)),
    GA("Georgia", GeoCoordinate(33.040619, -83.643074)),
    HI("Hawaii", GeoCoordinate(21.094318, -157.498337)),
    ID("Idaho", GeoCoordinate(44.240459, -114.478828)),
    IL("Illinois", GeoCoordinate(40.349457, -88.986137)),
    IN("Indiana", GeoCoordinate(39.849426, -86.258278)),
    IA("Iowa", GeoCoordinate(42.011539, -93.210526)),
    KS("Kansas", GeoCoordinate(38.526600, -96.726486)),
    KY("Kentucky", GeoCoordinate(37.668140, -84.670067)),
    LA("Louisiana", GeoCoordinate(31.169546, -91.867805)),
    ME("Maine", GeoCoordinate(44.693947, -69.381927)),
    MD("Maryland", GeoCoordinate(39.063946, -76.802101)),
    MA("Massachusetts", GeoCoordinate(42.230171, -71.530106)),
    MI("Michigan", GeoCoordinate(43.326618, -84.536095)),
    MN("Minnesota", GeoCoordinate(45.694454, -93.900192)),
    MS("Mississippi", GeoCoordinate(32.741646, -89.678696)),
    MO("Missouri", GeoCoordinate(38.456085, -92.288368)),
    MT("Montana", GeoCoordinate(46.921925, -110.454353)),
    NE("Nebraska", GeoCoordinate(41.125370, -98.268082)),
    NV("Nevada", GeoCoordinate(38.313515, -117.055374)),
    NH("New Hampshire", GeoCoordinate(43.452492, -71.563896)),
    NJ("New Jersey", GeoCoordinate(40.298904, -74.521011)),
    NM("New Mexico", GeoCoordinate(34.840515, -106.248482)),
    NY("New York", GeoCoordinate(42.165726, -74.948051)),
    NC("North Carolina", GeoCoordinate(35.630066, -79.806419)),
    ND("North Dakota", GeoCoordinate(47.528912, -99.784012)),
    OH("Ohio", GeoCoordinate(40.388783, -82.764915)),
    OK("Oklahoma", GeoCoordinate(35.565342, -96.928917)),
    OR("Oregon", GeoCoordinate(44.572021, -122.070938)),
    PA("Pennsylvania", GeoCoordinate(40.590752, -77.209755)),
    RI("Rhode Island", GeoCoordinate(41.680893, -71.511780)),
    SC("South Carolina", GeoCoordinate(33.856892, -80.945007)),
    SD("South Dakota", GeoCoordinate(44.299782, -99.438828)),
    TN("Tennessee", GeoCoordinate(35.747845, -86.692345)),
    TX("Texas", GeoCoordinate(31.054487, -97.563461)),
    UT("Utah", GeoCoordinate(40.150032, -111.862434)),
    VT("Vermont", GeoCoordinate(44.045876, -72.710686)),
    VA("Virginia", GeoCoordinate(37.769337, -78.169968)),
    WA("Washington", GeoCoordinate(47.400902, -121.490494)),
    WV("West Virginia", GeoCoordinate(38.491226, -80.954453)),
    WI("Wisconsin", GeoCoordinate(44.268543, -89.616508)),
    WY("Wyoming", GeoCoordinate(42.755966, -107.302490)),
}