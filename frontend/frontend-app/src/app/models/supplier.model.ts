export interface Supplier {
  id?: string;
  nit: string;
  businessName: string;
  email: string;
  phone?: string;
  personType: PersonType;
  status: SupplierStatus;
  // UI-only flag used to mark a newly created supplier that hasn't
  // been confirmed by the server (shown as a "Pendiente" badge).
  __pending?: boolean;
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
