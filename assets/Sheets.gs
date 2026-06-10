/**
 * GAS (Google Apps Script): Sheets.gs
 * Put this in your Google Sheet -> Extensions -> Apps Script.
 * Deploy as a Web App (set "Who has access" to "Anyone") to enable real-time automatic synchronization with your Android Dunia App!
 */

// Global constant for Sheet Names
var TRANSACTION_SHEET = "Transaksi";
var CONFIG_SHEET = "Config";
var GOALS_SHEET = "SavingGoals";

/**
 * Initializes the Spreadsheet with necessary columns if they don't exist.
 */
function initializeSheets() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  
  // 1. Transactions Sheet
  var transSheet = ss.getSheetByName(TRANSACTION_SHEET);
  if (!transSheet) {
    transSheet = ss.insertSheet(TRANSACTION_SHEET);
    transSheet.appendRow(["ID", "Waktu", "Pengguna", "Kategori", "Deskripsi", "Tipe", "Jumlah", "Timestamp"]);
    transSheet.getRange(1, 1, 1, 8).setFontWeight("bold").setBackground("#E2E8F0");
  }
  
  // 2. Config Sheet
  var confSheet = ss.getSheetByName(CONFIG_SHEET);
  if (!confSheet) {
    confSheet = ss.insertSheet(CONFIG_SHEET);
    confSheet.appendRow(["Key", "Value"]);
    confSheet.getRange(1, 1, 1, 2).setFontWeight("bold").setBackground("#E2E8F0");
  }

  // 3. Goals Sheet
  var goalsSheet = ss.getSheetByName(GOALS_SHEET);
  if (!goalsSheet) {
    goalsSheet = ss.insertSheet(GOALS_SHEET);
    goalsSheet.appendRow(["ID", "Name", "Target", "Current", "Deadline", "Done"]);
    goalsSheet.getRange(1, 1, 1, 6).setFontWeight("bold").setBackground("#E2E8F0");
  }
}

/**
 * Handles Web App POST Requests (from Android Kotlin client)
 */
function doPost(e) {
  try {
    initializeSheets();
    var postData = JSON.parse(e.postData.contents);
    var action = postData.action;
    
    var result = { success: false, message: "" };
    
    if (action === "sync_database") {
      result = syncDatabaseFromAndroid(postData.data);
    } else if (action === "insert_transaction") {
      result = addTransactionDirect(postData.data);
    } else {
      result.message = "Aksi tidak dikenali";
    }
    
    return ContentService.createTextOutput(JSON.stringify(result))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({ success: false, error: err.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

/**
 * Handles Web App GET Requests (to retrieve current Sheets data)
 */
function doGet(e) {
  try {
    initializeSheets();
    var action = e.parameter.action;
    var data = {};
    
    if (action === "get_all") {
      data = getAllDatabaseState();
    }
    
    return ContentService.createTextOutput(JSON.stringify({ success: true, data: data }))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({ success: false, error: err.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

// ============================================================================
// GOOGLE.SCRIPT.RUN COMPATIBLE FUNCTIONS (Supporting Client UI Sidebar / Callback)
// ============================================================================

/**
 * Returns complete database payload to the frontend.
 * Can be called asynchronously via google.script.run.withSuccessHandler(callback).getDatabaseStateForWeb()
 */
function getDatabaseStateForWeb() {
  try {
    return { success: true, payload: getAllDatabaseState() };
  } catch (err) {
    return { success: false, error: err.toString() };
  }
}

/**
 * Updates a single key-value config pair.
 * Can be called via google.script.run.withSuccessHandler(cb).updateConfigValue(key, value)
 */
function updateConfigValue(key, value) {
  try {
    initializeSheets();
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ss.getSheetByName(CONFIG_SHEET);
    var data = sheet.getDataRange().getValues();
    var foundRow = -1;
    
    for (var i = 1; i < data.length; i++) {
      if (data[i][0] === key) {
        foundRow = i + 1;
        break;
      }
    }
    
    if (foundRow !== -1) {
      sheet.getRange(foundRow, 2).setValue(value);
    } else {
      sheet.appendRow([key, value]);
    }
    return { success: true, key: key, value: value };
  } catch (err) {
    return { success: false, error: err.toString() };
  }
}

/**
 * Inserts or updates a transaction in real-time.
 * Can be called via google.script.run.withSuccessHandler(cb).updateTransaction(data)
 */
function updateTransaction(transactionObj) {
  try {
    initializeSheets();
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ss.getSheetByName(TRANSACTION_SHEET);
    var data = sheet.getDataRange().getValues();
    
    var id = transactionObj.id;
    var timestamp = transactionObj.timestamp || Date.now();
    var waktuReadable = Utilities.formatDate(new Date(timestamp), "GMT+7", "yyyy-MM-dd HH:mm:ss");
    
    var rowValues = [
      id,
      waktuReadable,
      transactionObj.user || "",
      transactionObj.category || "",
      transactionObj.description || "",
      transactionObj.type || "",
      transactionObj.amount || 0,
      timestamp
    ];
    
    var foundRow = -1;
    for (var i = 1; i < data.length; i++) {
      if (data[i][0].toString() === id.toString()) {
        foundRow = i + 1;
        break;
      }
    }
    
    if (foundRow !== -1) {
      sheet.getRange(foundRow, 1, 1, 8).setValues([rowValues]);
    } else {
      sheet.appendRow(rowValues);
    }
    
    return { success: true, itemId: id };
  } catch (err) {
    return { success: false, error: err.toString() };
  }
}

/**
 * Bulk synchronizes data structure directly into Google Sheets.
 * Can be called via google.script.run.withSuccessHandler(cb).syncAllDatabaseState(data)
 */
function syncAllDatabaseState(fullDbState) {
  try {
    var result = syncDatabaseFromAndroid(fullDbState);
    return result;
  } catch (err) {
    return { success: false, error: err.toString() };
  }
}

// ============================================================================
// HELPER INTERNAL TRANSFORMATION LOGIC
// ============================================================================

function getAllDatabaseState() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  
  var txSheet = ss.getSheetByName(TRANSACTION_SHEET);
  var cfgSheet = ss.getSheetByName(CONFIG_SHEET);
  var glSheet = ss.getSheetByName(GOALS_SHEET);
  
  var transactions = [];
  if (txSheet) {
    var vals = txSheet.getDataRange().getValues();
    for (var i = 1; i < vals.length; i++) {
      if (vals[i][0]) {
        transactions.push({
          id: vals[i][0],
          user: vals[i][2],
          category: vals[i][3],
          description: vals[i][4],
          type: vals[i][5],
          amount: Number(vals[i][6]),
          timestamp: Number(vals[i][7])
        });
      }
    }
  }
  
  var configs = {};
  if (cfgSheet) {
    var vals = cfgSheet.getDataRange().getValues();
    for (var i = 1; i < vals.length; i++) {
      if (vals[i][0]) {
        configs[vals[i][0]] = vals[i][1];
      }
    }
  }

  var savingGoals = [];
  if (glSheet) {
    var vals = glSheet.getDataRange().getValues();
    for (var i = 1; i < vals.length; i++) {
      if (vals[i][0]) {
        savingGoals.push({
          id: Number(vals[i][0]),
          name: vals[i][1],
          targetAmount: Number(vals[i][2]),
          currentAmount: Number(vals[i][3]),
          deadline: vals[i][4],
          isDone: vals[i][5] === true || vals[i][5].toString().toLowerCase() === "true"
        });
      }
    }
  }
  
  return {
    transactions: transactions,
    configs: configs,
    savingGoals: savingGoals
  };
}

function addTransactionDirect(tx) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName(TRANSACTION_SHEET);
  var timestamp = tx.timestamp || Date.now();
  var waktuReadable = Utilities.formatDate(new Date(timestamp), "GMT+7", "yyyy-MM-dd HH:mm:ss");
  
  sheet.appendRow([
    tx.id || ("tx_" + Date.now()),
    waktuReadable,
    tx.user || "Haikal",
    tx.category || "General",
    tx.description || "",
    tx.type || "PENGELUARAN",
    tx.amount || 0,
    timestamp
  ]);
  return { success: true, message: "Transaksi tersimpan di Google Sheet!" };
}

function syncDatabaseFromAndroid(dbObj) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  
  // 1. Sync Transactions
  var txSheet = ss.getSheetByName(TRANSACTION_SHEET);
  if (txSheet) {
    txSheet.clearContents();
    txSheet.appendRow(["ID", "Waktu", "Pengguna", "Kategori", "Deskripsi", "Tipe", "Jumlah", "Timestamp"]);
    if (dbObj.transactions && dbObj.transactions.length > 0) {
      dbObj.transactions.forEach(function(tx) {
        var ts = tx.timestamp || Date.now();
        var waktuReadable = Utilities.formatDate(new Date(ts), "GMT+7", "yyyy-MM-dd HH:mm:ss");
        txSheet.appendRow([
          tx.id || "",
          waktuReadable,
          tx.user || "",
          tx.category || "",
          tx.description || "",
          tx.type || "",
          tx.amount || 0,
          ts
        ]);
      });
    }
  }
  
  // 2. Sync Configs
  var cfgSheet = ss.getSheetByName(CONFIG_SHEET);
  if (cfgSheet) {
    cfgSheet.clearContents();
    cfgSheet.appendRow(["Key", "Value"]);
    if (dbObj.configs) {
      for (var key in dbObj.configs) {
        cfgSheet.appendRow([key, dbObj.configs[key]]);
      }
    }
  }

  // 3. Sync Saving Goals
  var glSheet = ss.getSheetByName(GOALS_SHEET);
  if (glSheet) {
    glSheet.clearContents();
    glSheet.appendRow(["ID", "Name", "Target", "Current", "Deadline", "Done"]);
    if (dbObj.savingGoals && dbObj.savingGoals.length > 0) {
      dbObj.savingGoals.forEach(function(g) {
        glSheet.appendRow([
          g.id || 0,
          g.name || "",
          g.targetAmount || 0,
          g.currentAmount || 0,
          g.deadline || "",
          g.isDone || false
        ]);
      });
    }
  }
  
  return { success: true, message: "Seluruh basis data berhasil di-sinkronisasi!" };
}
