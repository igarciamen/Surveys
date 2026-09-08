// Matches Spring Data's PagedModel JSON (metadata nested under `page`).
export interface PageInfo {
  size: number;
  number: number;
  totalElements: number;
  totalPages: number;
}

export interface PageResponse<T> {
  content: T[];
  page: PageInfo;
}
