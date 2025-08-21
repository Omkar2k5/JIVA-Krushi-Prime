# JIVA Room Database Setup Guide

## Overview
This document outlines the complete Room database setup generated from your MySQL schema to support all screens in your JIVA Android application.

## Database Tables Mapped

### 1. **users** → `UserEntity`
- **Purpose**: User authentication and management
- **Key Fields**: UserID, MobileNumber, CompanyName, CompanyCode, OwnerName
- **Used By**: Login screen, user session management

### 2. **tb_acmaster** → `AccountMasterEntity`
- **Purpose**: Customer/Party master data
- **Key Fields**: Ac_ID, Account_Name, Mobile, Area, Opening_Balance, Address
- **Used By**: Outstanding Report, WhatsApp Marketing, Ledger Report

### 3. **tb_closing_balance** → `ClosingBalanceEntity`
- **Purpose**: Outstanding balances for customers
- **Key Fields**: AC_ID, Account_Name, Balance, Days, Credit_Limit
- **Used By**: Outstanding Report Screen

### 4. **tb_stock** → `StockEntity`
- **Purpose**: Inventory management and stock tracking
- **Key Fields**: ITEM_ID, Item_Name, Opening, InWard, OutWard, Closing_Stock, Valuation
- **Used By**: Stock Report Screen

### 5. **tb_salepurchase** → `SalePurchaseEntity`
- **Purpose**: Sales and purchase transactions
- **Key Fields**: trDate, PartyName, Item_Name, Qty, Rate, Amount, trType
- **Used By**: Sales Report Screen, Day End Report

### 6. **tb_ledger** → `LedgerEntity`
- **Purpose**: Double-entry bookkeeping records
- **Key Fields**: EntryDate, Ac_ID, DR, CR, EntryType, Narration
- **Used By**: Ledger Report Screen

### 7. **tb_expiry** → `ExpiryEntity`
- **Purpose**: Item expiry tracking
- **Key Fields**: Item_ID, Item_Name, Batch_No, Expiry_Date, Qty, DaysLeft
- **Used By**: Expiry Report Screen

### 8. **tb_templates** → `TemplateEntity`
- **Purpose**: WhatsApp message templates
- **Key Fields**: TempID, Category, Msg, InstanceID, AccessToken
- **Used By**: WhatsApp Marketing Screen

## Screen-to-Database Mapping

| Screen | Primary Tables | Data Flow |
|--------|---------------|-----------|
| **Outstanding Report** | `tb_acmaster` + `tb_closing_balance` | Join accounts with balances |
| **Ledger Report** | `tb_ledger` + `tb_acmaster` | Filter ledger by account |
| **Stock Report** | `tb_stock` | Direct mapping |
| **Sales Report** | `tb_salepurchase` | Filter by date/party/item |
| **Day End Report** | `tb_salepurchase` + `tb_ledger` | Aggregate daily totals |
| **WhatsApp Marketing** | `tb_acmaster` + `tb_templates` | Customer contacts + templates |
| **Price Screen** | Custom API endpoint | `/api/priceScreen` |
| **Expiry Report** | `tb_expiry` | Filter by expiry dates |

## Implementation Files Created

### Entities (7 files)
- `UserEntity.kt`
- `AccountMasterEntity.kt`
- `ClosingBalanceEntity.kt`
- `StockEntity.kt`
- `SalePurchaseEntity.kt`
- `LedgerEntity.kt`
- `ExpiryEntity.kt`
- `TemplateEntity.kt`

### DAOs (7 files)
- `UserDao.kt`
- `AccountMasterDao.kt`
- `ClosingBalanceDao.kt`
- `StockDao.kt`
- `SalePurchaseDao.kt`
- `LedgerDao.kt`
- `ExpiryDao.kt`
- `TemplateDao.kt`

### Core Database Files
- `JivaDatabase.kt` - Main Room database class
- `Converters.kt` - Type converters for Date and BigDecimal
- `JivaRepository.kt` - Repository interface
- `JivaRepositoryImpl.kt` - Repository implementation
- `JivaApiService.kt` - Retrofit API interface
- `DataMapper.kt` - Maps entities to UI models

## Next Steps

1. **Add Room Dependencies** ✅ (Already added to build.gradle.kts)

2. **Create API Configuration**
   ```kotlin
   // Add your server base URL
   const val BASE_URL = "https://your-server.com/"
   ```

3. **Initialize Database in Application Class**
   ```kotlin
   class JivaApplication : Application() {
       val database by lazy { JivaDatabase.getDatabase(this) }
   }
   ```

4. **Update Existing Screens**
   - Replace dummy data with repository calls
   - Use ViewModels to manage data flow
   - Implement offline-first data loading

5. **Implement Data Sync Strategy**
   - Sync on app startup
   - Periodic background sync
   - Manual refresh options

## API Endpoints Mapped
- `GET /api/users` → UserEntity
- `GET /api/accounts` → AccountMasterEntity  
- `GET /api/closing-balances` → ClosingBalanceEntity
- `GET /api/stocks` → StockEntity
- `GET /api/sale-purchase` → SalePurchaseEntity
- `GET /api/ledgers` → LedgerEntity
- `GET /api/expiries` → ExpiryEntity
- `GET /api/templates` → TemplateEntity
- `GET /api/priceScreen` → Custom price data

## Benefits of This Setup
- **Offline-first**: App works without internet
- **Real-time sync**: Background data synchronization
- **Type-safe**: Compile-time query validation
- **Performance**: Local database queries are fast
- **Scalable**: Easy to add new tables and relationships
