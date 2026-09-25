import XCTest
import Capacitor
@testable import TextToSpeechPlugin

class TextToSpeechTests: XCTestCase {

    func testUnknownLanguageIsNotSupported() {
        let implementation = TextToSpeech()

        XCTAssertFalse(implementation.isLanguageSupported("not-a-language"))
    }
}

class TextToSpeechPluginTests: XCTestCase {

    func testEveryMethodIsRegisteredAsAPromise() {
        let methods = TextToSpeechPlugin().pluginMethods
        XCTAssertEqual(methods.map(\.name), [
            "speak", "stop", "openInstall", "getSupportedLanguages", "getSupportedVoices", "isLanguageSupported"
        ])
        XCTAssertTrue(methods.allSatisfy { $0.returnType == .promise })
    }

    func testSpeakingAnUnsupportedLanguageThrows() {
        let call = CAPPluginCall(callbackId: "test", methodName: "speak", options: ["text": "hello", "lang": "not-a-language"],
                                 success: { _, _ in
                                    XCTFail("speak must not resolve")
                                 }, error: { _ in
                                    XCTFail("speak answers by throwing")
                                 })
        XCTAssertThrowsError(try TextToSpeechPlugin().speak(call)) { error in
            XCTAssertEqual((error as? CAPPluginError)?.message, "This language is not supported.")
            XCTAssertNil((error as? CAPPluginError)?.code)
        }
    }
}
