export interface ContractCreateRequest {
  supplierId: string;
  object: string;
  budget: number;
  startDate: string;
  endDate: string;
}

export interface ContractResponse {
  id: string;
  supplierId: string;
  supplierNit?: string;
  supplierBusinessName?: string;
  contractNumber: string;
  object: string;
  budget: number;
  startDate: string;
  endDate: string;
  status: string;
  createdByUserId: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface PaginatedContractResponse {
  content: ContractResponse[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}