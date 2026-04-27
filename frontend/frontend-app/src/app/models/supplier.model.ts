export interface Supplier {
  id?: string;
  nit: string;
  businessName: string;
  email: string;
  phone?: string;
  personType: PersonType;
  status: SupplierStatus;
  createdAt?: string;
  updatedAt?: string;
}

export enum PersonType {
  JURIDICA = 'JURIDICA',
  NATURAL = 'NATURAL'
}

export enum SupplierStatus {
  HABILITADO = 'HABILITADO',
  INHABILITADO = 'INHABILITADO'
}

export interface SupplierPageResponse {
  content: Supplier[];
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
