export function saveBlob(blob: Blob, filename: string): void {
	const blobUrl = window.URL.createObjectURL(blob);
	const anchor = document.createElement('a');
	anchor.href = blobUrl;
	anchor.download = filename;
	anchor.click();
	URL.revokeObjectURL(blobUrl);
}
