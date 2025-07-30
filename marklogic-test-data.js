// 🎸 Epic MarkLogic Test Data Generator - Rush 2112 Style! 🎸
// Copy and paste this JavaScript into MarkLogic QConsole to load test documents

// 🚀 CRITICAL: Declare this as an update transaction
declareUpdate();

// Clear any existing test data first (optional)
// Uncomment the following lines if you want to clear existing test documents
/*
for (const uri of cts.uris(null, null, cts.collectionQuery(["red", "blue"]))) {
  xdmp.documentDelete(uri);
}
xdmp.log("🔥 Cleared existing test documents");
*/

// 🎸 Epic Red Collection Documents - Like Rush's Red Barchetta! 🎸
const redDocuments = [
  {
    uri: "/test-data/red/song1.json",
    content: {
      id: "red-001",
      title: "Tom Sawyer",
      artist: "Rush",
      album: "Moving Pictures",
      year: 1981,
      genre: "Progressive Rock",
      duration: 285,
      lyrics_sample: "A modern day warrior, mean mean stride",
      rating: 5,
      instruments: ["guitar", "bass", "drums", "keyboards"],
      collections: ["red"],
      metadata: {
        songwriter: ["Geddy Lee", "Alex Lifeson", "Neil Peart"],
        producer: "Terry Brown",
        studio: "Le Studio",
        country: "Canada"
      }
    }
  },
  {
    uri: "/test-data/red/song2.json",
    content: {
      id: "red-002",
      title: "Limelight",
      artist: "Rush",
      album: "Moving Pictures", 
      year: 1981,
      genre: "Progressive Rock",
      duration: 263,
      lyrics_sample: "Living on a lighted stage approaches the unreal",
      rating: 5,
      instruments: ["guitar", "bass", "drums"],
      collections: ["red"],
      metadata: {
        songwriter: ["Geddy Lee", "Alex Lifeson", "Neil Peart"],
        producer: "Terry Brown",
        studio: "Le Studio",
        country: "Canada"
      }
    }
  },
  {
    uri: "/test-data/red/product1.json",
    content: {
      id: "red-003",
      name: "Epic Guitar Amplifier",
      category: "Musical Equipment",
      price: 1299.99,
      brand: "RushTone",
      model: "2112-Pro",
      year: 2024,
      specifications: {
        watts: 100,
        tubes: ["12AX7", "EL34"],
        channels: 3,
        reverb: true
      },
      collections: ["red"],
      metadata: {
        manufacturer: "Progressive Audio",
        warranty: "5 years",
        country: "USA"
      }
    }
  },
  {
    uri: "/test-data/red/customer1.json",
    content: {
      id: "red-004",
      name: "Neil Percussion",
      email: "neil.drums@rush.com",
      age: 45,
      location: "Toronto, Canada",
      preferences: ["progressive rock", "complex rhythms", "philosophical lyrics"],
      purchase_history: [
        {
          item: "Drum Kit",
          price: 3500.00,
          date: "2024-01-15"
        },
        {
          item: "Cymbals Set",
          price: 850.00,
          date: "2024-02-20"
        }
      ],
      collections: ["red"],
      metadata: {
        customer_since: "2020-03-15",
        loyalty_tier: "Platinum",
        total_spent: 4350.00
      }
    }
  },
  {
    uri: "/test-data/red/article1.json",
    content: {
      id: "red-005",
      title: "The Evolution of Progressive Rock",
      author: "Music Historian",
      publication_date: "2024-06-01",
      category: "Music Analysis",
      content: "Progressive rock emerged in the late 1960s and reached its pinnacle with bands like Rush, Yes, and Genesis. The genre is characterized by complex compositions, virtuosic musicianship, and conceptual themes.",
      tags: ["progressive rock", "music history", "Rush", "complex compositions"],
      word_count: 2500,
      collections: ["red"],
      metadata: {
        publisher: "Rock Chronicles",
        language: "English",
        views: 15420
      }
    }
  }
];

// 🎸 Epic Blue Collection Documents - Like Rush's Blue Collar Man! 🎸
const blueDocuments = [
  {
    uri: "/test-data/blue/song3.json",
    content: {
      id: "blue-001",
      title: "Freewill",
      artist: "Rush",
      album: "Permanent Waves",
      year: 1980,
      genre: "Progressive Rock",
      duration: 320,
      lyrics_sample: "You can choose a ready guide in some celestial voice",
      rating: 5,
      instruments: ["guitar", "bass", "drums"],
      collections: ["blue"],
      metadata: {
        songwriter: ["Geddy Lee", "Alex Lifeson", "Neil Peart"],
        producer: "Terry Brown",
        studio: "Advision Studios",
        country: "Canada"
      }
    }
  },
  {
    uri: "/test-data/blue/song4.json",
    content: {
      id: "blue-002",
      title: "The Spirit of Radio",
      artist: "Rush",
      album: "Permanent Waves",
      year: 1980,
      genre: "Progressive Rock",
      duration: 297,
      lyrics_sample: "Begin the day with a friendly voice",
      rating: 4,
      instruments: ["guitar", "bass", "drums"],
      collections: ["blue"],
      metadata: {
        songwriter: ["Geddy Lee", "Alex Lifeson", "Neil Peart"],
        producer: "Terry Brown",
        studio: "Advision Studios",
        country: "Canada"
      }
    }
  },
  {
    uri: "/test-data/blue/product2.json",
    content: {
      id: "blue-003",
      name: "Synthesizer Workstation",
      category: "Musical Equipment",
      price: 2499.99,
      brand: "ProgreSynth",
      model: "Geddy-2112",
      year: 2024,
      specifications: {
        keys: 88,
        voices: 256,
        presets: 1000,
        sequencer: true
      },
      collections: ["blue"],
      metadata: {
        manufacturer: "Electronic Music Co",
        warranty: "3 years",
        country: "Japan"
      }
    }
  },
  {
    uri: "/test-data/blue/customer2.json",
    content: {
      id: "blue-004",
      name: "Alex Strings",
      email: "alex.guitar@rush.com",
      age: 38,
      location: "Vancouver, Canada",
      preferences: ["guitar solos", "intricate compositions", "vintage gear"],
      purchase_history: [
        {
          item: "Electric Guitar",
          price: 2200.00,
          date: "2024-03-10"
        },
        {
          item: "Effects Pedals",
          price: 450.00,
          date: "2024-04-15"
        }
      ],
      collections: ["blue"],
      metadata: {
        customer_since: "2021-07-20",
        loyalty_tier: "Gold",
        total_spent: 2650.00
      }
    }
  },
  {
    uri: "/test-data/blue/event1.json",
    content: {
      id: "blue-005",
      title: "Progressive Rock Festival 2024",
      location: "Toronto Music Centre",
      date: "2024-08-15",
      time: "19:00",
      category: "Music Event",
      description: "A celebration of progressive rock featuring tribute bands and original compositions inspired by the greatest prog rock legends.",
      ticket_price: 75.00,
      capacity: 5000,
      featured_bands: ["Rush Tribute", "Genesis Revival", "Yes Reimagined"],
      collections: ["blue"],
      metadata: {
        organizer: "Prog Rock Productions",
        venue_type: "Indoor Arena",
        age_restriction: "All Ages"
      }
    }
  }
];

// 🎸 Function to insert documents with collections - Epic style! 🎸
function insertDocuments(documents, collectionName) {
  let insertedCount = 0;
  
  for (const doc of documents) {
    try {
      // Insert the document with the specified collection
      xdmp.documentInsert(
        doc.uri,
        doc.content,
        {
          collections: [collectionName],
          permissions: [
            xdmp.permission("rest-reader", "read"),
            xdmp.permission("rest-writer", "update")
          ]
        }
      );
      
      insertedCount++;
      xdmp.log(`🎸 Inserted document: ${doc.uri} into collection: ${collectionName}`);
      
    } catch (error) {
      xdmp.log(`🔥 Error inserting ${doc.uri}: ${error.message}`);
    }
  }
  
  return insertedCount;
}

// 🎸 Execute the epic data loading! 🎸
try {
  xdmp.log("🎸 Starting epic test data loading - Rush 2112 style! 🎸");
  
  // Insert red collection documents
  const redInserted = insertDocuments(redDocuments, "red");
  xdmp.log(`🔥 Successfully inserted ${redInserted} documents into RED collection`);
  
  // Insert blue collection documents  
  const blueInserted = insertDocuments(blueDocuments, "blue");
  xdmp.log(`🔥 Successfully inserted ${blueInserted} documents into BLUE collection`);
  
  // Summary
  const totalInserted = redInserted + blueInserted;
  xdmp.log(`🎸 EPIC SUCCESS! Total documents inserted: ${totalInserted}`);
  xdmp.log(`🎸 Red collection: ${redInserted} documents`);
  xdmp.log(`🎸 Blue collection: ${blueInserted} documents`);
  
  // Verify the data was inserted
  const redCount = cts.estimate(cts.collectionQuery("red"));
  const blueCount = cts.estimate(cts.collectionQuery("blue"));
  
  xdmp.log(`🚀 Verification - Red collection count: ${redCount}`);
  xdmp.log(`🚀 Verification - Blue collection count: ${blueCount}`);
  
  xdmp.log("🎸 Test data loading complete - Rock on! 🎸");
  
} catch (error) {
  xdmp.log(`💥 Epic failure during data loading: ${error.message}`);
  throw error;
}

// 🎸 Optional: Display sample queries you can run to test the data
xdmp.log("\n🎸 SAMPLE QUERIES TO TEST YOUR DATA: 🎸");
xdmp.log("1. Count red documents: cts.estimate(cts.collectionQuery('red'))");
xdmp.log("2. Count blue documents: cts.estimate(cts.collectionQuery('blue'))");
xdmp.log("3. Search for Rush songs: cts.search(cts.andQuery([cts.collectionQuery(['red','blue']), cts.wordQuery('Rush')]))");
xdmp.log("4. Find documents by year: cts.search(cts.jsonPropertyValueQuery('year', 1981))");
xdmp.log("5. Search in red collection only: cts.search(cts.andQuery([cts.collectionQuery('red'), cts.wordQuery('guitar')]))");
xdmp.log("\n🚀 Your MarkLogic database is now rocking with test data! 🚀");
