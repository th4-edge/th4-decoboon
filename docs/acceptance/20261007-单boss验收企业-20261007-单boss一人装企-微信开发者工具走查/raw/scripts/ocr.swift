import AppKit
import Vision

guard CommandLine.arguments.count > 1 else {
    fputs("usage: swift ocr.swift <image>\n", stderr)
    exit(2)
}
let url = URL(fileURLWithPath: CommandLine.arguments[1])
guard let image = NSImage(contentsOf: url),
      let cgImage = image.cgImage(forProposedRect: nil, context: nil, hints: nil) else {
    fputs("unable to load image\n", stderr)
    exit(3)
}
let request = VNRecognizeTextRequest()
request.recognitionLevel = .accurate
request.usesLanguageCorrection = true
request.recognitionLanguages = ["zh-Hans", "en-US"]
let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
try handler.perform([request])
let observations = (request.results ?? []).sorted {
    if abs($0.boundingBox.midY - $1.boundingBox.midY) > 0.02 {
        return $0.boundingBox.midY > $1.boundingBox.midY
    }
    return $0.boundingBox.minX < $1.boundingBox.minX
}
for observation in observations {
    guard let candidate = observation.topCandidates(1).first else { continue }
    let box = observation.boundingBox
    print(String(format: "%.3f %.3f %.3f %.3f\t%@", box.minX, box.minY, box.width, box.height, candidate.string))
}
