import XCTest
@testable import TextToSpeechPlugin

class TextToSpeechTests: XCTestCase {

    func testUnknownLanguageIsNotSupported() {
        let implementation = TextToSpeech()

        XCTAssertFalse(implementation.isLanguageSupported("not-a-language"))
    }
}
