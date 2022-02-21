package com.clearspend.capital.data.model.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public enum MerchantType {
  UNKNOWN("unkown", "Placeholder for any new merchant type we don't support", "0"),
  AC_REFRIGERATION_REPAIR("ac_refrigeration_repair", "A/C, Refrigeration Repair", "7623"),
  ACCOUNTING_BOOKKEEPING_SERVICES(
      "accounting_bookkeeping_services", "Accounting/Bookkeeping Services", "8931"),
  ADVERTISING_SERVICES("advertising_services", "Advertising Services", "7311"),
  AGRICULTURAL_COOPERATIVE("agricultural_cooperative", "Agricultural Cooperative", "0763"),
  AIRLINES_AIR_CARRIERS("airlines_air_carriers", "Airlines, Air Carriers", "4511"),
  AIRPORTS_FLYING_FIELDS("airports_flying_fields", "Airports, Flying Fields", "4582"),
  AMBULANCE_SERVICES("ambulance_services", "Ambulance Services", "4119"),
  AMUSEMENT_PARKS_CARNIVALS("amusement_parks_carnivals", "Amusement Parks/Carnivals", "7996"),
  ANTIQUE_REPRODUCTIONS("antique_reproductions", "Antique Reproductions", "5937"),
  ANTIQUE_SHOPS("antique_shops", "Antique Shops", "5932"),
  AQUARIUMS("aquariums", "Aquariums", "7998"),
  ARCHITECTURAL_SURVEYING_SERVICES(
      "architectural_surveying_services", "Architectural/Surveying Services", "8911"),
  ART_DEALERS_AND_GALLERIES("art_dealers_and_galleries", "Art Dealers and Galleries", "5971"),
  ARTISTS_SUPPLY_AND_CRAFT_SHOPS(
      "artists_supply_and_craft_shops", "Artists Supply and Craft Shops", "5970"),
  AUTO_BODY_REPAIR_SHOPS("auto_body_repair_shops", "Auto Body Repair Shops", "7531"),
  AUTO_PAINT_SHOPS("auto_paint_shops", "Auto Paint Shops", "7535"),
  AUTO_SERVICE_SHOPS("auto_service_shops", "Auto Service Shops", "7538"),
  AUTO_AND_HOME_SUPPLY_STORES("auto_and_home_supply_stores", "Auto and Home Supply Stores", "5531"),
  AUTOMATED_CASH_DISBURSE("automated_cash_disburse", "Automated Cash Disburse", "6011"),
  AUTOMATED_FUEL_DISPENSERS("automated_fuel_dispensers", "Automated Fuel Dispensers", "5542"),
  AUTOMOBILE_ASSOCIATIONS("automobile_associations", "Automobile Associations", "8675"),
  AUTOMOTIVE_PARTS_AND_ACCESSORIES_STORES(
      "automotive_parts_and_accessories_stores", "Automotive Parts and Accessories Stores", "5533"),
  AUTOMOTIVE_TIRE_STORES("automotive_tire_stores", "Automotive Tire Stores", "5532"),
  BAIL_AND_BOND_PAYMENTS(
      "bail_and_bond_payments",
      "Bail and Bond Payments (payment to the surety for the bond, not the actual bond paid to the government agency)",
      "9223"),
  BAKERIES("bakeries", "Bakeries", "5462"),
  BANDS_ORCHESTRAS("bands_orchestras", "Bands, Orchestras", "7929"),
  BARBER_AND_BEAUTY_SHOPS("barber_and_beauty_shops", "Barber and Beauty Shops", "7230"),
  BETTING_CASINO_GAMBLING("betting_casino_gambling", "Betting/Casino Gambling", "7995"),
  BICYCLE_SHOPS("bicycle_shops", "Bicycle Shops", "5940"),
  BILLIARD_POOL_ESTABLISHMENTS(
      "billiard_pool_establishments", "Billiard/Pool Establishments", "7932"),
  BOAT_DEALERS("boat_dealers", "Boat Dealers", "5551"),
  BOAT_RENTALS_AND_LEASES("boat_rentals_and_leases", "Boat Rentals and Leases", "4457"),
  BOOK_STORES("book_stores", "Book Stores", "5942"),
  BOOKS_PERIODICALS_AND_NEWSPAPERS(
      "books_periodicals_and_newspapers", "Books, Periodicals, and Newspapers", "5192"),
  BOWLING_ALLEYS("bowling_alleys", "Bowling Alleys", "7933"),
  BUS_LINES("bus_lines", "Bus Lines", "4131"),
  BUSINESS_SECRETARIAL_SCHOOLS(
      "business_secretarial_schools", "Business/Secretarial Schools", "8244"),
  BUYING_SHOPPING_SERVICES("buying_shopping_services", "Buying/Shopping Services", "7278"),
  CABLE_SATELLITE_AND_OTHER_PAY_TELEVISION_AND_RADIO(
      "cable_satellite_and_other_pay_television_and_radio",
      "Cable, Satellite, and Other Pay Television and Radio",
      "4899"),
  CAMERA_AND_PHOTOGRAPHIC_SUPPLY_STORES(
      "camera_and_photographic_supply_stores", "Camera and Photographic Supply Stores", "5946"),
  CANDY_NUT_AND_CONFECTIONERY_STORES(
      "candy_nut_and_confectionery_stores", "Candy, Nut, and Confectionery Stores", "5441"),
  CAR_RENTAL_AGENCIES("car_rental_agencies", "Car Rental Agencies", "7512"),
  CAR_WASHES("car_washes", "Car Washes", "7542"),
  CAR_AND_TRUCK_DEALERS_NEW_USED(
      "car_and_truck_dealers_new_used",
      "Car and Truck Dealers (New & Used) Sales, Service, Repairs Parts and Leasing",
      "5511"),
  CAR_AND_TRUCK_DEALERS_USED_ONLY(
      "car_and_truck_dealers_used_only",
      "Car and Truck Dealers (Used Only) Sales, Service, Repairs Parts and Leasing",
      "5521"),
  CARPENTRY_SERVICES("carpentry_services", "Carpentry Services", "1750"),
  CARPET_UPHOLSTERY_CLEANING("carpet_upholstery_cleaning", "Carpet/Upholstery Cleaning", "7217"),
  CATERERS("caterers", "Caterers", "5811"),
  CHARITABLE_AND_SOCIAL_SERVICE_ORGANIZATIONS_FUNDRAISING(
      "charitable_and_social_service_organizations_fundraising",
      "Charitable and Social Service Organizations - Fundraising",
      "8398"),
  CHEMICALS_AND_ALLIED_PRODUCTS(
      "chemicals_and_allied_products",
      "Chemicals and Allied Products (Not Elsewhere Classified)",
      "5169"),
  CHILD_CARE_SERVICES("child_care_services", "Child Care Services", "8351"),
  CHILDRENS_AND_INFANTS_WEAR_STORES(
      "childrens_and_infants_wear_stores", "Childrens and Infants Wear Stores", "5641"),
  CHIROPODISTS_PODIATRISTS("chiropodists_podiatrists", "Chiropodists, Podiatrists", "8049"),
  CHIROPRACTORS("chiropractors", "Chiropractors", "8041"),
  CIGAR_STORES_AND_STANDS("cigar_stores_and_stands", "Cigar Stores and Stands", "5993"),
  CIVIC_SOCIAL_FRATERNAL_ASSOCIATIONS(
      "civic_social_fraternal_associations", "Civic, Social, Fraternal Associations", "8641"),
  CLEANING_AND_MAINTENANCE("cleaning_and_maintenance", "Cleaning and Maintenance", "7349"),
  CLOTHING_RENTAL("clothing_rental", "Clothing Rental", "7296"),
  COLLEGES_UNIVERSITIES("colleges_universities", "Colleges, Universities", "8220"),
  COMMERCIAL_EQUIPMENT(
      "commercial_equipment", "Commercial Equipment (Not Elsewhere Classified)", "5046"),
  COMMERCIAL_FOOTWEAR("commercial_footwear", "Commercial Footwear", "5139"),
  COMMERCIAL_PHOTOGRAPHY_ART_AND_GRAPHICS(
      "commercial_photography_art_and_graphics",
      "Commercial Photography, Art and Graphics",
      "7333"),
  COMMUTER_TRANSPORT_AND_FERRIES(
      "commuter_transport_and_ferries", "Commuter Transport, Ferries", "4111"),
  COMPUTER_NETWORK_SERVICES("computer_network_services", "Computer Network Services", "4816"),
  COMPUTER_PROGRAMMING("computer_programming", "Computer Programming", "7372"),
  COMPUTER_REPAIR("computer_repair", "Computer Repair", "7379"),
  COMPUTER_SOFTWARE_STORES("computer_software_stores", "Computer Software Stores", "5734"),
  COMPUTERS_PERIPHERALS_AND_SOFTWARE(
      "computers_peripherals_and_software", "Computers, Peripherals, and Software", "5045"),
  CONCRETE_WORK_SERVICES("concrete_work_services", "Concrete Work Services", "1771"),
  CONSTRUCTION_MATERIALS(
      "construction_materials", "Construction Materials (Not Elsewhere Classified)", "5039"),
  CONSULTING_PUBLIC_RELATIONS(
      "consulting_public_relations", "Consulting, Public Relations", "7392"),
  CORRESPONDENCE_SCHOOLS("correspondence_schools", "Correspondence Schools", "8241"),
  COSMETIC_STORES("cosmetic_stores", "Cosmetic Stores", "5977"),
  COUNSELING_SERVICES("counseling_services", "Counseling Services", "7277"),
  COUNTRY_CLUBS("country_clubs", "Country Clubs", "7997"),
  COURIER_SERVICES("courier_services", "Courier Services", "4215"),
  COURT_COSTS(
      "court_costs", "Court Costs, Including Alimony and Child Support - Courts of Law", "9211"),
  CREDIT_REPORTING_AGENCIES("credit_reporting_agencies", "Credit Reporting Agencies", "7321"),
  CRUISE_LINES("cruise_lines", "Cruise Lines", "4411"),
  DAIRY_PRODUCTS_STORES("dairy_products_stores", "Dairy Products Stores", "5451"),
  DANCE_HALL_STUDIOS_SCHOOLS("dance_hall_studios_schools", "Dance Hall, Studios, Schools", "7911"),
  DATING_ESCORT_SERVICES("dating_escort_services", "Dating/Escort Services", "7273"),
  DENTISTS_ORTHODONTISTS("dentists_orthodontists", "Dentists, Orthodontists", "8021"),
  DEPARTMENT_STORES("department_stores", "Department Stores", "5311"),
  DETECTIVE_AGENCIES("detective_agencies", "Detective Agencies", "7393"),
  DIGITAL_GOODS_MEDIA("digital_goods_media", "Digital Goods Media – Books, Movies, Music", "5815"),
  DIGITAL_GOODS_APPLICATIONS(
      "digital_goods_applications", "Digital Goods – Applications (Excludes Games)", "5817"),
  DIGITAL_GOODS_GAMES("digital_goods_games", "Digital Goods – Games", "5816"),
  DIGITAL_GOODS_LARGE_VOLUME(
      "digital_goods_large_volume", "Digital Goods – Large Digital Goods Merchant", "5818"),
  DIRECT_MARKETING_CATALOG_MERCHANT(
      "direct_marketing_catalog_merchant", "Direct Marketing - Catalog Merchant", "5964"),
  DIRECT_MARKETING_COMBINATION_CATALOG_AND_RETAIL_MERCHANT(
      "direct_marketing_combination_catalog_and_retail_merchant",
      "Direct Marketing - Combination Catalog and Retail Merchant",
      "5965"),
  DIRECT_MARKETING_INBOUND_TELEMARKETING(
      "direct_marketing_inbound_telemarketing", "Direct Marketing - Inbound Telemarketing", "5967"),
  DIRECT_MARKETING_INSURANCE_SERVICES(
      "direct_marketing_insurance_services", "Direct Marketing - Insurance Services", "5960"),
  DIRECT_MARKETING_OTHER("direct_marketing_other", "Direct Marketing - Other", "5969"),
  DIRECT_MARKETING_OUTBOUND_TELEMARKETING(
      "direct_marketing_outbound_telemarketing",
      "Direct Marketing - Outbound Telemarketing",
      "5966"),
  DIRECT_MARKETING_SUBSCRIPTION(
      "direct_marketing_subscription", "Direct Marketing - Subscription", "5968"),
  DIRECT_MARKETING_TRAVEL("direct_marketing_travel", "Direct Marketing - Travel", "5962"),
  DISCOUNT_STORES("discount_stores", "Discount Stores", "5310"),
  DOCTORS("doctors", "Doctors", "8011"),
  DOOR_TO_DOOR_SALES("door_to_door_sales", "Door-To-Door Sales", "5963"),
  DRAPERY_WINDOW_COVERING_AND_UPHOLSTERY_STORES(
      "drapery_window_covering_and_upholstery_stores",
      "Drapery, Window Covering, and Upholstery Stores",
      "5714"),
  DRINKING_PLACES("drinking_places", "Drinking Places", "5813"),
  DRUG_STORES_AND_PHARMACIES("drug_stores_and_pharmacies", "Drug Stores and Pharmacies", "5912"),
  DRUGS_DRUG_PROPRIETARIES_AND_DRUGGIST_SUNDRIES(
      "drugs_drug_proprietaries_and_druggist_sundries",
      "Drugs, Drug Proprietaries, and Druggist Sundries",
      "5122"),
  DRY_CLEANERS("dry_cleaners", "Dry Cleaners", "7216"),
  DURABLE_GOODS("durable_goods", "Durable Goods (Not Elsewhere Classified)", "5099"),
  DUTY_FREE_STORES("duty_free_stores", "Duty Free Stores", "5309"),
  EATING_PLACES_RESTAURANTS("eating_places_restaurants", "Eating Places, Restaurants", "5812"),
  EDUCATIONAL_SERVICES("educational_services", "Educational Services", "8299"),
  ELECTRIC_RAZOR_STORES("electric_razor_stores", "Electric Razor Stores", "5997"),
  ELECTRICAL_PARTS_AND_EQUIPMENT(
      "electrical_parts_and_equipment", "Electrical Parts and Equipment", "5065"),
  ELECTRICAL_SERVICES("electrical_services", "Electrical Services", "1731"),
  ELECTRONICS_REPAIR_SHOPS("electronics_repair_shops", "Electronics Repair Shops", "7622"),
  ELECTRONICS_STORES("electronics_stores", "Electronics Stores", "5732"),
  ELEMENTARY_SECONDARY_SCHOOLS(
      "elementary_secondary_schools", "Elementary, Secondary Schools", "8211"),
  EMPLOYMENT_TEMP_AGENCIES("employment_temp_agencies", "Employment/Temp Agencies", "7361"),
  EQUIPMENT_RENTAL("equipment_rental", "Equipment Rental", "7394"),
  EXTERMINATING_SERVICES("exterminating_services", "Exterminating Services", "7342"),
  FAMILY_CLOTHING_STORES("family_clothing_stores", "Family Clothing Stores", "5651"),
  FAST_FOOD_RESTAURANTS("fast_food_restaurants", "Fast Food Restaurants", "5814"),
  FINANCIAL_INSTITUTIONS("financial_institutions", "Financial Institutions", "6012"),
  FINES_GOVERNMENT_ADMINISTRATIVE_ENTITIES(
      "fines_government_administrative_entities",
      "Fines - Government Administrative Entities",
      "9222"),
  FIREPLACE_FIREPLACE_SCREENS_AND_ACCESSORIES_STORES(
      "fireplace_fireplace_screens_and_accessories_stores",
      "Fireplace, Fireplace Screens, and Accessories Stores",
      "5718"),
  FLOOR_COVERING_STORES("floor_covering_stores", "Floor Covering Stores", "5713"),
  FLORISTS("florists", "Florists", "5992"),
  FLORISTS_SUPPLIES_NURSERY_STOCK_AND_FLOWERS(
      "florists_supplies_nursery_stock_and_flowers",
      "Florists Supplies, Nursery Stock, and Flowers",
      "5193"),
  FREEZER_AND_LOCKER_MEAT_PROVISIONERS(
      "freezer_and_locker_meat_provisioners", "Freezer and Locker Meat Provisioners", "5422"),
  FUEL_DEALERS_NON_AUTOMOTIVE(
      "fuel_dealers_non_automotive", "Fuel Dealers (Non Automotive)", "5983"),
  FUNERAL_SERVICES_CREMATORIES(
      "funeral_services_crematories", "Funeral Services, Crematories", "7261"),
  FURNITURE_REPAIR_REFINISHING(
      "furniture_repair_refinishing", "Furniture Repair, Refinishing", "7641"),
  FURNITURE_HOME_FURNISHINGS_AND_EQUIPMENT_STORES_EXCEPT_APPLIANCES(
      "furniture_home_furnishings_and_equipment_stores_except_appliances",
      "Furniture, Home Furnishings, and Equipment Stores, Except Appliances",
      "5712"),
  FURRIERS_AND_FUR_SHOPS("furriers_and_fur_shops", "Furriers and Fur Shops", "5681"),
  GENERAL_SERVICES("general_services", "General Services", "1520"),
  GIFT_CARD_NOVELTY_AND_SOUVENIR_SHOPS(
      "gift_card_novelty_and_souvenir_shops", "Gift, Card, Novelty, and Souvenir Shops", "5947"),
  GLASS_PAINT_AND_WALLPAPER_STORES(
      "glass_paint_and_wallpaper_stores", "Glass, Paint, and Wallpaper Stores", "5231"),
  GLASSWARE_CRYSTAL_STORES("glassware_crystal_stores", "Glassware, Crystal Stores", "5950"),
  GOLF_COURSES_PUBLIC("golf_courses_public", "Golf Courses - Public", "7992"),
  GOVERNMENT_SERVICES(
      "government_services", "Government Services (Not Elsewhere Classified)", "9399"),
  GROCERY_STORES_SUPERMARKETS(
      "grocery_stores_supermarkets", "Grocery Stores, Supermarkets", "5411"),
  HARDWARE_STORES("hardware_stores", "Hardware Stores", "5251"),
  HARDWARE_EQUIPMENT_AND_SUPPLIES(
      "hardware_equipment_and_supplies", "Hardware, Equipment, and Supplies", "5072"),
  HEALTH_AND_BEAUTY_SPAS("health_and_beauty_spas", "Health and Beauty Spas", "7298"),
  HEARING_AIDS_SALES_AND_SUPPLIES(
      "hearing_aids_sales_and_supplies", "Hearing Aids Sales and Supplies", "5975"),
  HEATING_PLUMBING_A_C("heating_plumbing_a_c", "Heating, Plumbing, A/C", "1711"),
  HOBBY_TOY_AND_GAME_SHOPS("hobby_toy_and_game_shops", "Hobby, Toy, and Game Shops", "5945"),
  HOME_SUPPLY_WAREHOUSE_STORES(
      "home_supply_warehouse_stores", "Home Supply Warehouse Stores", "5200"),
  HOSPITALS("hospitals", "Hospitals", "8062"),
  HOTELS_MOTELS_AND_RESORTS("hotels_motels_and_resorts", "Hotels, Motels, and Resorts", "7011"),
  HOUSEHOLD_APPLIANCE_STORES("household_appliance_stores", "Household Appliance Stores", "5722"),
  INDUSTRIAL_SUPPLIES(
      "industrial_supplies", "Industrial Supplies (Not Elsewhere Classified)", "5085"),
  INFORMATION_RETRIEVAL_SERVICES(
      "information_retrieval_services", "Information Retrieval Services", "7375"),
  INSURANCE_DEFAULT("insurance_default", "Insurance - Default", "6399"),
  INSURANCE_UNDERWRITING_PREMIUMS(
      "insurance_underwriting_premiums", "Insurance Underwriting, Premiums", "6300"),
  INTRA_COMPANY_PURCHASES("intra_company_purchases", "Intra-Company Purchases", "9950"),
  JEWELRY_STORES_WATCHES_CLOCKS_AND_SILVERWARE_STORES(
      "jewelry_stores_watches_clocks_and_silverware_stores",
      "Jewelry Stores, Watches, Clocks, and Silverware Stores",
      "5944"),
  LANDSCAPING_SERVICES("landscaping_services", "Landscaping Services", "0780"),
  LAUNDRIES("laundries", "Laundries", "7211"),
  LAUNDRY_CLEANING_SERVICES("laundry_cleaning_services", "Laundry, Cleaning Services", "7210"),
  LEGAL_SERVICES_ATTORNEYS("legal_services_attorneys", "Legal Services, Attorneys", "8111"),
  LUGGAGE_AND_LEATHER_GOODS_STORES(
      "luggage_and_leather_goods_stores", "Luggage and Leather Goods Stores", "5948"),
  LUMBER_BUILDING_MATERIALS_STORES(
      "lumber_building_materials_stores", "Lumber, Building Materials Stores", "5211"),
  MANUAL_CASH_DISBURSE("manual_cash_disburse", "Manual Cash Disburse", "6010"),
  MARINAS_SERVICE_AND_SUPPLIES(
      "marinas_service_and_supplies", "Marinas, Service and Supplies", "4468"),
  MASONRY_STONEWORK_AND_PLASTER(
      "masonry_stonework_and_plaster", "Masonry, Stonework, and Plaster", "1740"),
  MASSAGE_PARLORS("massage_parlors", "Massage Parlors", "7297"),
  MEDICAL_SERVICES("medical_services", "Medical Services", "8099"),
  MEDICAL_AND_DENTAL_LABS("medical_and_dental_labs", "Medical and Dental Labs", "8071"),
  MEDICAL_DENTAL_OPHTHALMIC_AND_HOSPITAL_EQUIPMENT_AND_SUPPLIES(
      "medical_dental_ophthalmic_and_hospital_equipment_and_supplies",
      "Medical, Dental, Ophthalmic, and Hospital Equipment and Supplies",
      "5047"),
  MEMBERSHIP_ORGANIZATIONS("membership_organizations", "Membership Organizations", "8699"),
  MENS_AND_BOYS_CLOTHING_AND_ACCESSORIES_STORES(
      "mens_and_boys_clothing_and_accessories_stores",
      "Mens and Boys Clothing and Accessories Stores",
      "5611"),
  MENS_WOMENS_CLOTHING_STORES(
      "mens_womens_clothing_stores", "Mens, Womens Clothing Stores", "5691"),
  METAL_SERVICE_CENTERS("metal_service_centers", "Metal Service Centers", "5051"),
  MISCELLANEOUS_APPAREL_AND_ACCESSORY_SHOPS(
      "miscellaneous_apparel_and_accessory_shops",
      "Miscellaneous Apparel and Accessory Shops",
      "5699"),
  MISCELLANEOUS_AUTO_DEALERS("miscellaneous_auto_dealers", "Miscellaneous Auto Dealers", "5599"),
  MISCELLANEOUS_BUSINESS_SERVICES(
      "miscellaneous_business_services", "Miscellaneous Business Services", "7399"),
  MISCELLANEOUS_FOOD_STORES(
      "miscellaneous_food_stores",
      "Miscellaneous Food Stores - Convenience Stores and Specialty Markets",
      "5499"),
  MISCELLANEOUS_GENERAL_MERCHANDISE(
      "miscellaneous_general_merchandise", "Miscellaneous General Merchandise", "5399"),
  MISCELLANEOUS_GENERAL_SERVICES(
      "miscellaneous_general_services", "Miscellaneous General Services", "7299"),
  MISCELLANEOUS_HOME_FURNISHING_SPECIALTY_STORES(
      "miscellaneous_home_furnishing_specialty_stores",
      "Miscellaneous Home Furnishing Specialty Stores",
      "5719"),
  MISCELLANEOUS_PUBLISHING_AND_PRINTING(
      "miscellaneous_publishing_and_printing", "Miscellaneous Publishing and Printing", "2741"),
  MISCELLANEOUS_RECREATION_SERVICES(
      "miscellaneous_recreation_services", "Miscellaneous Recreation Services", "7999"),
  MISCELLANEOUS_REPAIR_SHOPS("miscellaneous_repair_shops", "Miscellaneous Repair Shops", "7699"),
  MISCELLANEOUS_SPECIALTY_RETAIL(
      "miscellaneous_specialty_retail", "Miscellaneous Specialty Retail", "5999"),
  MOBILE_HOME_DEALERS("mobile_home_dealers", "Mobile Home Dealers", "5271"),
  MOTION_PICTURE_THEATERS("motion_picture_theaters", "Motion Picture Theaters", "7832"),
  MOTOR_FREIGHT_CARRIERS_AND_TRUCKING(
      "motor_freight_carriers_and_trucking",
      "Motor Freight Carriers and Trucking - Local and Long Distance, Moving and Storage Companies, and Local Delivery Services",
      "4214"),
  MOTOR_HOMES_DEALERS("motor_homes_dealers", "Motor Homes Dealers", "5592"),
  MOTOR_VEHICLE_SUPPLIES_AND_NEW_PARTS(
      "motor_vehicle_supplies_and_new_parts", "Motor Vehicle Supplies and New Parts", "5013"),
  MOTORCYCLE_SHOPS_AND_DEALERS(
      "motorcycle_shops_and_dealers", "Motorcycle Shops and Dealers", "5571"),
  MOTORCYCLE_SHOPS_DEALERS("motorcycle_shops_dealers", "Motorcycle Shops, Dealers", "5561"),
  MUSIC_STORES_MUSICAL_INSTRUMENTS_PIANOS_AND_SHEET_MUSIC(
      "music_stores_musical_instruments_pianos_and_sheet_music",
      "Music Stores-Musical Instruments, Pianos, and Sheet Music",
      "5733"),
  NEWS_DEALERS_AND_NEWSSTANDS("news_dealers_and_newsstands", "News Dealers and Newsstands", "5994"),
  NON_FI_MONEY_ORDERS("non_fi_money_orders", "Non-FI, Money Orders", "6051"),
  NON_FI_STORED_VALUE_CARD_PURCHASE_LOAD(
      "non_fi_stored_value_card_purchase_load", "Non-FI, Stored Value Card Purchase/Load", "6540"),
  NONDURABLE_GOODS("nondurable_goods", "Nondurable Goods (Not Elsewhere Classified)", "5199"),
  NURSERIES_LAWN_AND_GARDEN_SUPPLY_STORES(
      "nurseries_lawn_and_garden_supply_stores",
      "Nurseries, Lawn and Garden Supply Stores",
      "5261"),
  NURSING_PERSONAL_CARE("nursing_personal_care", "Nursing/Personal Care", "8050"),
  OFFICE_AND_COMMERCIAL_FURNITURE(
      "office_and_commercial_furniture", "Office and Commercial Furniture", "5021"),
  OPTICIANS_EYEGLASSES("opticians_eyeglasses", "Opticians, Eyeglasses", "8043"),
  OPTOMETRISTS_OPHTHALMOLOGIST(
      "optometrists_ophthalmologist", "Optometrists, Ophthalmologist", "8042"),
  ORTHOPEDIC_GOODS_PROSTHETIC_DEVICES(
      "orthopedic_goods_prosthetic_devices", "Orthopedic Goods - Prosthetic Devices", "5976"),
  OSTEOPATHS("osteopaths", "Osteopaths", "8031"),
  PACKAGE_STORES_BEER_WINE_AND_LIQUOR(
      "package_stores_beer_wine_and_liquor", "Package Stores-Beer, Wine, and Liquor", "5921"),
  PAINTS_VARNISHES_AND_SUPPLIES(
      "paints_varnishes_and_supplies", "Paints, Varnishes, and Supplies", "5198"),
  PARKING_LOTS_GARAGES("parking_lots_garages", "Parking Lots, Garages", "7523"),
  PASSENGER_RAILWAYS("passenger_railways", "Passenger Railways", "4112"),
  PAWN_SHOPS("pawn_shops", "Pawn Shops", "5933"),
  PET_SHOPS_PET_FOOD_AND_SUPPLIES(
      "pet_shops_pet_food_and_supplies", "Pet Shops, Pet Food, and Supplies", "5995"),
  PETROLEUM_AND_PETROLEUM_PRODUCTS(
      "petroleum_and_petroleum_products", "Petroleum and Petroleum Products", "5172"),
  PHOTO_DEVELOPING("photo_developing", "Photo Developing", "7395"),
  PHOTOGRAPHIC_STUDIOS("photographic_studios", "Photographic Studios", "7221"),
  PHOTOGRAPHIC_PHOTOCOPY_MICROFILM_EQUIPMENT_AND_SUPPLIES(
      "photographic_photocopy_microfilm_equipment_and_supplies",
      "Photographic, Photocopy, Microfilm Equipment, and Supplies",
      "5044"),
  PICTURE_VIDEO_PRODUCTION("picture_video_production", "Picture/Video Production", "7829"),
  PIECE_GOODS_NOTIONS_AND_OTHER_DRY_GOODS(
      "piece_goods_notions_and_other_dry_goods",
      "Piece Goods, Notions, and Other Dry Goods",
      "5131"),
  PLUMBING_HEATING_EQUIPMENT_AND_SUPPLIES(
      "plumbing_heating_equipment_and_supplies",
      "Plumbing, Heating Equipment, and Supplies",
      "5074"),
  POLITICAL_ORGANIZATIONS("political_organizations", "Political Organizations", "8651"),
  POSTAL_SERVICES_GOVERNMENT_ONLY(
      "postal_services_government_only", "Postal Services - Government Only", "9402"),
  PRECIOUS_STONES_AND_METALS_WATCHES_AND_JEWELRY(
      "precious_stones_and_metals_watches_and_jewelry",
      "Precious Stones and Metals, Watches and Jewelry",
      "5094"),
  PROFESSIONAL_SERVICES("professional_services", "Professional Services", "8999"),
  PUBLIC_WAREHOUSING_AND_STORAGE(
      "public_warehousing_and_storage",
      "Public Warehousing and Storage - Farm Products, Refrigerated Goods, Household Goods, and Storage",
      "4225"),
  QUICK_COPY_REPRO_AND_BLUEPRINT(
      "quick_copy_repro_and_blueprint", "Quick Copy, Repro, and Blueprint", "7338"),
  RAILROADS("railroads", "Railroads", "4011"),
  REAL_ESTATE_AGENTS_AND_MANAGERS_RENTALS(
      "real_estate_agents_and_managers_rentals",
      "Real Estate Agents and Managers - Rentals",
      "6513"),
  RECORD_STORES("record_stores", "Record Stores", "5735"),
  RECREATIONAL_VEHICLE_RENTALS(
      "recreational_vehicle_rentals", "Recreational Vehicle Rentals", "7519"),
  RELIGIOUS_GOODS_STORES("religious_goods_stores", "Religious Goods Stores", "5973"),
  RELIGIOUS_ORGANIZATIONS("religious_organizations", "Religious Organizations", "8661"),
  ROOFING_SIDING_SHEET_METAL("roofing_siding_sheet_metal", "Roofing/Siding, Sheet Metal", "1761"),
  SECRETARIAL_SUPPORT_SERVICES(
      "secretarial_support_services", "Secretarial Support Services", "7339"),
  SECURITY_BROKERS_DEALERS("security_brokers_dealers", "Security Brokers/Dealers", "6211"),
  SERVICE_STATIONS("service_stations", "Service Stations", "5541"),
  SEWING_NEEDLEWORK_FABRIC_AND_PIECE_GOODS_STORES(
      "sewing_needlework_fabric_and_piece_goods_stores",
      "Sewing, Needlework, Fabric, and Piece Goods Stores",
      "5949"),
  SHOE_REPAIR_HAT_CLEANING("shoe_repair_hat_cleaning", "Shoe Repair/Hat Cleaning", "7251"),
  SHOE_STORES("shoe_stores", "Shoe Stores", "5661"),
  SMALL_APPLIANCE_REPAIR("small_appliance_repair", "Small Appliance Repair", "7629"),
  SNOWMOBILE_DEALERS("snowmobile_dealers", "Snowmobile Dealers", "5598"),
  SPECIAL_TRADE_SERVICES("special_trade_services", "Special Trade Services", "1799"),
  SPECIALTY_CLEANING("specialty_cleaning", "Specialty Cleaning", "2842"),
  SPORTING_GOODS_STORES("sporting_goods_stores", "Sporting Goods Stores", "5941"),
  SPORTING_RECREATION_CAMPS("sporting_recreation_camps", "Sporting/Recreation Camps", "7032"),
  SPORTS_CLUBS_FIELDS("sports_clubs_fields", "Sports Clubs/Fields", "7941"),
  SPORTS_AND_RIDING_APPAREL_STORES(
      "sports_and_riding_apparel_stores", "Sports and Riding Apparel Stores", "5655"),
  STAMP_AND_COIN_STORES("stamp_and_coin_stores", "Stamp and Coin Stores", "5972"),
  STATIONARY_OFFICE_SUPPLIES_PRINTING_AND_WRITING_PAPER(
      "stationary_office_supplies_printing_and_writing_paper",
      "Stationary, Office Supplies, Printing and Writing Paper",
      "5111"),
  STATIONERY_STORES_OFFICE_AND_SCHOOL_SUPPLY_STORES(
      "stationery_stores_office_and_school_supply_stores",
      "Stationery Stores, Office, and School Supply Stores",
      "5943"),
  SWIMMING_POOLS_SALES("swimming_pools_sales", "Swimming Pools Sales", "5996"),
  T_UI_TRAVEL_GERMANY("t_ui_travel_germany", "TUI Travel - Germany", "4723"),
  TAILORS_ALTERATIONS("tailors_alterations", "Tailors, Alterations", "5697"),
  TAX_PAYMENTS_GOVERNMENT_AGENCIES(
      "tax_payments_government_agencies", "Tax Payments - Government Agencies", "9311"),
  TAX_PREPARATION_SERVICES("tax_preparation_services", "Tax Preparation Services", "7276"),
  TAXICABS_LIMOUSINES("taxicabs_limousines", "Taxicabs/Limousines", "4121"),
  TELECOMMUNICATION_EQUIPMENT_AND_TELEPHONE_SALES(
      "telecommunication_equipment_and_telephone_sales",
      "Telecommunication Equipment and Telephone Sales",
      "4812"),
  TELECOMMUNICATION_SERVICES("telecommunication_services", "Telecommunication Services", "4814"),
  TELEGRAPH_SERVICES("telegraph_services", "Telegraph Services", "4821"),
  TENT_AND_AWNING_SHOPS("tent_and_awning_shops", "Tent and Awning Shops", "5998"),
  TESTING_LABORATORIES("testing_laboratories", "Testing Laboratories", "8734"),
  THEATRICAL_TICKET_AGENCIES("theatrical_ticket_agencies", "Theatrical Ticket Agencies", "7922"),
  TIMESHARES("timeshares", "Timeshares", "7012"),
  TIRE_RETREADING_AND_REPAIR("tire_retreading_and_repair", "Tire Retreading and Repair", "7534"),
  TOLLS_BRIDGE_FEES("tolls_bridge_fees", "Tolls/Bridge Fees", "4784"),
  TOURIST_ATTRACTIONS_AND_EXHIBITS(
      "tourist_attractions_and_exhibits", "Tourist Attractions and Exhibits", "7991"),
  TOWING_SERVICES("towing_services", "Towing Services", "7549"),
  TRAILER_PARKS_CAMPGROUNDS("trailer_parks_campgrounds", "Trailer Parks, Campgrounds", "7033"),
  TRANSPORTATION_SERVICES(
      "transportation_services", "Transportation Services (Not Elsewhere Classified)", "4789"),
  TRAVEL_AGENCIES_TOUR_OPERATORS(
      "travel_agencies_tour_operators", "Travel Agencies, Tour Operators", "4722"),
  TRUCK_STOP_ITERATION("truck_stop_iteration", "Truck StopIteration", "7511"),
  TRUCK_UTILITY_TRAILER_RENTALS(
      "truck_utility_trailer_rentals", "Truck/Utility Trailer Rentals", "7513"),
  TYPESETTING_PLATE_MAKING_AND_RELATED_SERVICES(
      "typesetting_plate_making_and_related_services",
      "Typesetting, Plate Making, and Related Services",
      "2791"),
  TYPEWRITER_STORES("typewriter_stores", "Typewriter Stores", "5978"),
  U_S_FEDERAL_GOVERNMENT_AGENCIES_OR_DEPARTMENTS(
      "u_s_federal_government_agencies_or_departments",
      "U.S. Federal Government Agencies or Departments",
      "9405"),
  UNIFORMS_COMMERCIAL_CLOTHING(
      "uniforms_commercial_clothing", "Uniforms, Commercial Clothing", "5137"),
  USED_MERCHANDISE_AND_SECONDHAND_STORES(
      "used_merchandise_and_secondhand_stores", "Used Merchandise and Secondhand Stores", "5931"),
  UTILITIES("utilities", "Utilities", "4900"),
  VARIETY_STORES("variety_stores", "Variety Stores", "5331"),
  VETERINARY_SERVICES("veterinary_services", "Veterinary Services", "0742"),
  VIDEO_AMUSEMENT_GAME_SUPPLIES(
      "video_amusement_game_supplies", "Video Amusement Game Supplies", "7993"),
  VIDEO_GAME_ARCADES("video_game_arcades", "Video Game Arcades", "7994"),
  VIDEO_TAPE_RENTAL_STORES("video_tape_rental_stores", "Video Tape Rental Stores", "7841"),
  VOCATIONAL_TRADE_SCHOOLS("vocational_trade_schools", "Vocational/Trade Schools", "8249"),
  WATCH_JEWELRY_REPAIR("watch_jewelry_repair", "Watch/Jewelry Repair", "7631"),
  WELDING_REPAIR("welding_repair", "Welding Repair", "7692"),
  WHOLESALE_CLUBS("wholesale_clubs", "Wholesale Clubs", "5300"),
  WIG_AND_TOUPEE_STORES("wig_and_toupee_stores", "Wig and Toupee Stores", "5698"),
  WIRES_MONEY_ORDERS("wires_money_orders", "Wires, Money Orders", "4829"),
  WOMENS_ACCESSORY_AND_SPECIALTY_SHOPS(
      "womens_accessory_and_specialty_shops", "Womens Accessory and Specialty Shops", "5631"),
  WOMENS_READY_TO_WEAR_STORES("womens_ready_to_wear_stores", "Womens Ready-To-Wear Stores", "5621"),
  WRECKING_AND_SALVAGE_YARDS("wrecking_and_salvage_yards", "Wrecking and Salvage Yards", "5935"),
  ;

  private final String stripeMerchantType;
  private final String description;
  private final String mcc;

  private static final Map<String, MerchantType> stripeMerchantTypeToMerchantTypeMap =
      initializeMap();
  private static final Map<String, MerchantType> mccToMerchantTypeMap = initializeMccMap();

  private static Map<String, MerchantType> initializeMap() {
    return Arrays.stream(MerchantType.values())
        .collect(Collectors.toUnmodifiableMap(e -> e.stripeMerchantType, Function.identity()));
  }

  private static Map<String, MerchantType> initializeMccMap() {
    return Arrays.stream(MerchantType.values())
        .collect(Collectors.toUnmodifiableMap(e -> e.mcc, Function.identity()));
  }

  MerchantType(String stripeMerchantType, String description, String mcc) {
    this.stripeMerchantType = stripeMerchantType;
    this.description = description;
    this.mcc = mcc;
  }

  public static MerchantType fromStripe(String stripeMerchantType) {
    MerchantType eventType = stripeMerchantTypeToMerchantTypeMap.get(stripeMerchantType);
    return eventType != null ? eventType : UNKNOWN;
  }

  public static MerchantType fromMccCode(String mccCode) {
    MerchantType eventType = mccToMerchantTypeMap.get(mccCode);
    return eventType != null ? eventType : UNKNOWN;
  }

  public String getMcc() {
    return mcc;
  }

  public String getStripeMerchantType() {
    return stripeMerchantType;
  }

  public String getDescription() {
    return description;
  }
}
